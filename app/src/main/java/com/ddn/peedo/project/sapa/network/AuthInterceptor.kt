package com.ddn.peedo.project.sapa.network

import com.ddn.peedo.project.sapa.store.TokenManager
import android.content.Context
import com.ddn.peedo.project.sapa.LoginActivity
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import android.content.Intent

class AuthInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val token = runBlocking {
            TokenManager(context).getToken()
        }

        val request = if (!token.isNullOrEmpty()) {
            chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        // 🔐 GLOBAL AUTH GUARD (Middleware behavior)
        if (response.code == 401) {
            runBlocking {
                TokenManager(context).clearToken()
            }

            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}
