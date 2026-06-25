package com.ddn.peedo.project.sapa

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.databinding.ActivityLoginBinding
import com.ddn.peedo.project.sapa.model.AuthRequest
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.ui.dashboard.MainDashboard
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import kotlinx.coroutines.launch
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import cn.pedant.SweetAlert.SweetAlertDialog
import com.ddn.peedo.project.sapa.databinding.DialogAboutAppBinding
import com.ddn.peedo.project.sapa.databinding.DialogScanQrBinding
import com.ddn.peedo.project.sapa.databinding.DialogStudentsBinding
import com.ddn.peedo.project.sapa.model.Settings
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.utils.JwtUtils
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onInit()

    }

    private fun onInit() {
        with(binding) {

            btnSubmit.setOnClickListener {
                attemptLogin()
            }

            sapaWebApp.setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://davnorsystems.gov.ph/SAPA/login")
                )
                startActivity(intent)
            }

            btnInfo.setOnClickListener {

                val binding = DialogAboutAppBinding.inflate(LayoutInflater.from(this@LoginActivity))
//                val binding = DialogScanQrBinding.inflate(layoutInflater)

                val dialog = Dialog(this@LoginActivity).apply {
                    setContentView(binding.root)
                    setCancelable(true)
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    window?.setLayout(
                        (context.resources.displayMetrics.widthPixels * 0.92).toInt(), // 92% width
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                binding.btnClose.setOnClickListener { dialog.dismiss() }
                dialog.show()
            }
        }
    }

    private fun attemptLogin() {

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

        val username = binding.username.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            SweetAlertUtil.showError(
                this,
                "Login Failed",
                "Username and password are required."
            )
            return
        }

        val loading = SweetAlertUtil.showLoading(
            this,
            "Authenticating",
            "Please wait..."
        )
        val session = SessionManager(this)

        /*
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api(this@LoginActivity).authenticate(
                    AuthRequest(username, password)
                )

                Log.d("LoginActivity_INFO", "Attempting Login...")

                if (response.isSuccessful && response.body() != null) {


                    Log.d("LoginActivity_INFO", "Login Success...")

                    val auth = response.body()!!
                    val token = auth.token


                    Log.d("LoginActivity_INFO", "Saving token...")
                    // 1️⃣ Save token
                    session.saveToken(token)

                    // 2️⃣ Decode token
                    val payload = JwtUtils.decode(token)

                    val username = payload.optString("unique_name", "")
                    val userId = payload.optString("nameid", "")
                    val roleId = payload.optString("role", "")

                    Log.d(
                        "LoginActivity_INFO",
                        "Login username=$username userId=$userId role=$roleId"
                    )


                    Log.d(
                        "LoginActivity_INFO",
                        "Fetching User details"
                    )

                    val userResponse = RetrofitClient.api(this@LoginActivity)
                        .getUserByUsername(username)


                    if (userResponse.isSuccessful && response.body() != null) {

                        val user = userResponse.body()

                        if (user != null) {

                            Log.d("LoginActivity_INFO", "User found: ${user.username}")

                            val userJson = JSONObject(Gson().toJson(user))
                            session.saveUser(userJson)

                            // ✅ privileges
                            val privResponse = RetrofitClient.api(this@LoginActivity)
                                .getPrivilegeByRole(user.roleID)

                            if (privResponse.isSuccessful && privResponse.body() != null) {

                                val privJson = JSONArray(Gson().toJson(privResponse.body()))
                                session.savePrivileges(privJson)

//                                loading.dismissWithAnimation()

                                // 4️⃣ Navigate
                                SweetAlertUtil.showSuccess(
                                    this@LoginActivity,
                                    "Welcome",
                                    auth.message
                                ) {
                                    goToDashboard()
                                }
                            }

                        } else {
//                            loading.dismissWithAnimation()
//                            delay(300)
                            Log.e("LoginActivity_INFO", "User is NULL (possible API mismatch)")
                            SweetAlertUtil.showError(this@LoginActivity, "Error", "User not found")
                        }

                    } else {
//                        loading.dismissWithAnimation()
//                        delay(300)

                        val errorMsg = response.errorBody()?.string()
                            ?: "Invalid credentials"

                        Toast.makeText(
                            this@LoginActivity,
                            extractMessage(errorMsg),
                            Toast.LENGTH_LONG
                        ).show()

                        Log.d("LoginActivity_INFO", "User no Successful : $errorMsg")
                        SweetAlertUtil.showError(
                            this@LoginActivity,
                            "User not found!",
                            extractMessage(errorMsg),
                            loading,
                        )
                    }
//
//                    if (JwtUtils.isTokenExpired(token)) {
//                        session.clearSession()
//                        SweetAlertUtil.showError(
//                            this@LoginActivity,
//                            "Session Error",
//                            "Token expired. Please login again."
//                        )
//                        return@launch
//                    }


                } else {
//                    loading.dismissWithAnimation()
//                    delay(300)

                    val errorMsg = response.errorBody()?.string()
                        ?: "Invalid credentials"

                    Toast.makeText(
                        this@LoginActivity,
                        extractMessage(errorMsg),
                        Toast.LENGTH_LONG
                    ).show()

                    Log.d("LoginActivity_INFO", "Login no Successful : $errorMsg")
                    SweetAlertUtil.showError(
                        this@LoginActivity,
                        "UnAuthenticated User!",
                        extractMessage(errorMsg),
                        loading,
                    )
                }

            } catch (e: Exception) {
//                loading.dismissWithAnimation()
//                delay(300)
                Log.d("LoginActivity_INFO", "Exception: ${e.message}")

                Toast.makeText(
                    this@LoginActivity,
                    "Unable to login",
                    Toast.LENGTH_LONG
                ).show()
                SweetAlertUtil.showError(
                    this@LoginActivity,
                    "Error",
                    "Unable to connect to login.",
                    loading,
                )
            }
        }
        */

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api(this@LoginActivity).authenticate(
                    AuthRequest(username, password)
                )

                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    val token = auth.token
                    session.saveToken(token)

                    val payload = JwtUtils.decode(token)
                    val username = payload.optString("unique_name", "")
                    val userId = payload.optString("nameid", "")

                    val userResponse =
                        RetrofitClient.api(this@LoginActivity).getUserByUsername(username)

                    if (userResponse.isSuccessful && userResponse.body() != null) {
                        val user = userResponse.body()!!
                        val userJson = JSONObject(Gson().toJson(user))
                        session.saveUser(userJson)

                        val privResponse =
                            RetrofitClient.api(this@LoginActivity).getPrivilegeByRole(user.roleID)

                        if (privResponse.isSuccessful && privResponse.body() != null) {
                            val privJson = JSONArray(Gson().toJson(privResponse.body()))
                            session.savePrivileges(privJson)

                            // ✅ Fetch and cache Settings (non-fatal if it fails)
                            fetchAndCacheSettings(this@LoginActivity)

                            withContext(Dispatchers.Main) { // ✅ UI on main thread
                                loading.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                                loading.titleText = "Welcome"
                                loading.contentText = auth.message
                                loading.setCancelable(true)
                                loading.setConfirmClickListener {
                                    it.dismissWithAnimation()
                                    goToDashboard()
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { // ✅ UI on main thread
                            loading.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                            loading.titleText = "Error"
                            loading.contentText = "User not found"
                            loading.setCancelable(true)
                            loading.setConfirmClickListener { it.dismissWithAnimation() }
                        }
                    }

                } else {
                    val errorMsg = withContext(Dispatchers.IO) {
                        response.errorBody()?.string() ?: "Invalid credentials"
                    }

                    withContext(Dispatchers.Main) {
                        loading.dismiss()
                    }

                    delay(500)

                    withContext(Dispatchers.Main) {
                        SweetAlertUtil.showError(
                            this@LoginActivity,
                            "Login Failed",
                            extractMessage(errorMsg)
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.dismiss()
                }
                delay(500)
                withContext(Dispatchers.Main) {
                    SweetAlertUtil.showError(
                        this@LoginActivity,
                        "Error",
                        "Unable to connect."
                    )
                }
            }
        }
    }

    suspend fun fetchAndCacheSettings(context: Context) {
        val sessionManager = SessionManager(context)
        try {
            val response = RetrofitClient.create(context).getSettings()
            if (response.isSuccessful) {
                Log.d("Settings_INFO", "Settings fetched and cached successfully ${response.body()}")
                val settingsList = response.body() ?: emptyList<Settings>()
                sessionManager.saveSettings(settingsList)

                Log.d("Settings_INFO", "Settings fetched and cached successfully $settingsList")
            } else {
                Log.d("Settings_INFO", "Failed to fetch settings: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("Settings_INFO", "Error fetching settings", e)
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, MainDashboard::class.java))
        finish()
    }

    // Extract { "message": "..." } from API
    private fun extractMessage(raw: String): String {
        return raw.substringAfter("message\":\"")
            .substringBefore("\"")
    }
}