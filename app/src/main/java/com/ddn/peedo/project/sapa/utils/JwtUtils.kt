package com.ddn.peedo.project.sapa.utils

import android.util.Base64
import android.util.Log
import org.json.JSONObject

object JwtUtils {

    fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE)
            )

            val json = JSONObject(payload)
            val exp = json.getLong("exp") * 1000 // seconds → millis

            System.currentTimeMillis() > exp
        } catch (e: Exception) {
            true
        }
    }

//    fun isTokenExpired(token: String): Boolean {
//        return try {
//            val payload = decode(token)
//            val exp = payload.optLong("exp", 0L) * 1000
//            exp == 0L || System.currentTimeMillis() > exp
//        } catch (e: Exception) {
//            Log.e("Token", "expired", e)
//            true
//        }
//    }


//    fun decode(token: String): JSONObject {
//        val parts = token.split(".")
//        if (parts.size != 3) {
//            throw IllegalArgumentException("Invalid JWT token")
//        }
//
//        val payload = parts[1]
//        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
//        val decodedString = String(decodedBytes)
//
//        return JSONObject(decodedString)
//    }

    fun decode(token: String): JSONObject {
        return try {
            val parts = token.split(".")
            require(parts.size == 3) { "Invalid JWT format" }

            val decodedBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )

            val decodedString = String(decodedBytes, Charsets.UTF_8)
            JSONObject(decodedString)

        } catch (e: Exception) {
            Log.e("JwtUtils", "JWT decode failed", e)
            throw e
        }
    }

    // ADD THIS — returns the token's expiry as epoch millis, or null if it can't be read
    fun getExpiryMillis(token: String): Long? {
        return try {
            val json = decode(token)
            json.optLong("exp", 0L).takeIf { it > 0 }?.times(1000)
        } catch (e: Exception) {
            null
        }
    }
}
