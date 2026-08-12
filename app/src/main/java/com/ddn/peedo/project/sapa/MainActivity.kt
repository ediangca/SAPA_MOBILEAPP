package com.ddn.peedo.project.sapa

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ddn.peedo.project.sapa.databinding.ActivityMainBinding
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.ui.dashboard.MainDashboard
import com.ddn.peedo.project.sapa.utils.JwtUtils
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.provider.Settings
import com.ddn.peedo.project.sapa.utils.ConnectivityUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // How long a session stays usable OFFLINE after its JWT technically expires.
    // Tune this to your rotation schedule realities — e.g. a week covers most
    // hospital placements without forcing daily reconnects.
    private val OFFLINE_GRACE_PERIOD_MS = 7 * 24 * 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Room creates the DB file lazily on first touch — this line alone
        // guarantees "create if not exists" with zero extra branching needed.
        lifecycleScope.launch {
            val db = SapaDatabase.getInstance(this@MainActivity)
            val userCount = db.userDao().getAllOnce().size
            Log.d("SapaDatabase", "Local DB ready. Cached users: $userCount")
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            if (!ConnectivityUtils.isNetworkAvailable(this@MainActivity)) {
                Log.d("MainActivity_INFO", "NO NETWORK AVAILABLE — checking local session")
                handleOfflineEntry()
            } else {
                checkApiAndProceed()
            }
        }
    }

    /**
     * Decides what to do when there's no network:
     * - Valid or grace-period session cached → let them into the dashboard.
     * - Nothing usable cached → tell them to connect for first-time setup.
     */
    private suspend fun handleOfflineEntry() {
        val session = SessionManager(this@MainActivity)
        val token = session.getToken()

        if (token.isNullOrEmpty()) {
            showConnectRequiredAlert(
                "No account found on this device. Please connect to the " +
                        "internet to log in for the first time."
            )
            return
        }

        val isExpired = JwtUtils.isTokenExpired(token)
        val withinGracePeriod = isExpired &&
                JwtUtils.getExpiryMillis(token)?.let {
                    (System.currentTimeMillis() - it) < OFFLINE_GRACE_PERIOD_MS
                } == true

        if (!isExpired || withinGracePeriod) {
            Toast.makeText(
                this@MainActivity,
                "You're offline — showing cached data",
                Toast.LENGTH_SHORT
            ).show()
            goToDashboard()
        } else {
            showConnectRequiredAlert(
                "Your session has expired and you've been offline too long. " +
                        "Please connect to the internet to log in again."
            )
        }
    }

    private fun showConnectRequiredAlert(message: String) {
        SweetAlertUtil.showConfirm(
            this@MainActivity,
            "INTERNET CONNECTIVITY REQUIRED",
            message,
            confirmText = "Retry",
            cancelText = "Settings",
            onConfirm = { recreate() },
            onCancel = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        )
    }

    private fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
        return false
    }

    private fun checkApiAndProceed() {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api(this@MainActivity).getSAPAInformation(1)
                if (response.isSuccessful && response.body() != null) {
                    checkAuthState()
                } else {
                    SweetAlertUtil.showError(
                        this@MainActivity, "Error",
                        "Server returned an error \n Please contact System Administrator"
                    )
                }
            } catch (e: Exception) {
                Log.d("MainActivity_INFO", "checkApiAndProceed: ${e.message}")
                // Network reports "on" but server unreachable — fall back the same way.
                handleOfflineEntry()
            }
        }
    }

    private fun checkAuthState() {
        lifecycleScope.launch {
            delay(1200)
            val session = SessionManager(this@MainActivity)
            val token = session.getToken()
            if (token.isNullOrEmpty()) {
                goToLogin(); return@launch
            }
            if (JwtUtils.isTokenExpired(token)) {
                session.clearSession()
                goToLogin()
            } else {
                goToDashboard()
            }
        }
    }

    private fun showLoading() { binding.progressBar.visibility = View.VISIBLE }
    private fun goToLogin() {
        binding.progressBar.visibility = View.GONE
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    private fun goToDashboard() {
        startActivity(Intent(this, MainDashboard::class.java))
        finish()
    }
}