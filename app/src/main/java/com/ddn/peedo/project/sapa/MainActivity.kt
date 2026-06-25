package com.ddn.peedo.project.sapa

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ddn.peedo.project.sapa.databinding.ActivityMainBinding
import android.content.Intent
import android.content.pm.PackageManager
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.ui.dashboard.MainDashboard
import com.ddn.peedo.project.sapa.utils.JwtUtils
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var hasRouted = false   // 🔒 prevents rerun
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onStart() {
        super.onStart()

        if (!isNetworkAvailable(this)) {
            Log.d("MainActivity_INFO", "NO NETWORK AVAILABLE")

            SweetAlertUtil.showConfirm(
                this,
                "INTERNET CONNECTIVITY REQUIRED",
                "Please turn on your Wi-Fi or mobile data to use this app.",
                confirmText = "Retry",
                cancelText = "Settings",
                onConfirm = {
                    // IF user taps "Retry"
                    recreate()
                },
                onCancel = {
                    // IF user taps "Settings"
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            )

            return
        } else {
            checkApiAndProceed()
        }
    }

    private fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    private fun checkApiAndProceed() {

//        val loadingDialog = SweetAlertUtil.showLoading(
//            this,
//            "Checking Server",
//            "Connecting to SAPA API..."
//        )

        showLoading()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api(this@MainActivity).getSAPAInformation(1)


//                loadingDialog.dismissWithAnimation()

                if (response.isSuccessful && response.body() != null) {
                    val info = response.body()!!
                    val rawJson = response.errorBody()?.string()
                        ?: response.body().toString()

                    Log.d("MainActivity_INFO", "Response ${response}")
                    Log.d("MainActivity_INFO", "Information ${info.toString()}")
                    Log.d("MainActivity_INFO", "ErrorBody ${rawJson}")
//                    SweetAlertUtil.showSuccess(
//                        this@MainActivity,
//                        "Success",
//                        "Server Connected!"
//                    ) {
//                    }


                    checkAuthState()
                } else {
                    SweetAlertUtil.showError(
                        this@MainActivity,
                        "Error",
                        "Server returned an error \n Please contact System Administrator"
                    )
                }

            } catch (e: Exception) {
//                loadingDialog.dismissWithAnimation()
                Log.d("MainActivity_INFO", "checkApiAndProceed: ${e.message}")

                Toast.makeText(
                    this@MainActivity,
                    "Unable to connect to server",
                    Toast.LENGTH_LONG
                ).show()
                SweetAlertUtil.showError(
                    this@MainActivity,
                    "Connection Failed",
                    "Unable to connect to server due to ${e.message}"
                ) {
                    goToLogin()
                }
            }
        }
    }

    private fun checkAuthState() {
        lifecycleScope.launch {
            delay(1200) // optional splash delay

            val session = SessionManager(this@MainActivity)
            val token = session.getToken()

            if (token.isNullOrEmpty()) {
                goToLogin()
                return@launch
            }
            // 🔑 JWT LOCAL EXPIRATION CHECK (LIKE WEB)
            if (JwtUtils.isTokenExpired(token)) {
                session.clearSession()
                goToLogin()
            } else {
                goToDashboard()
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

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