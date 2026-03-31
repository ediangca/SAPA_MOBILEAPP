package com.ddn.peedo.project.sapa.store

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("auth_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val USER_KEY = stringPreferencesKey("user")
        private val PRIVILEGE_KEY = stringPreferencesKey("privileges")
    }

    // ✅ Save Token
    suspend fun saveToken(token: String) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    // ✅ Save User (JSON)
    suspend fun saveUser(user: JSONObject) {
        Log.d("SessionManager", "Saving user: $user")
        context.dataStore.edit {
            it[USER_KEY] = user.toString()
        }
    }

    suspend fun getUser(): JSONObject? {
        val json = context.dataStore.data.first()[USER_KEY]
        return json?.let { JSONObject(it) }
    }

    // ✅ Save Privileges (JSONArray)
    suspend fun savePrivileges(privs: JSONArray) {
        Log.d("SessionManager", "Saving privileges: $privs")
        context.dataStore.edit {
            it[PRIVILEGE_KEY] = privs.toString()
        }
    }

    suspend fun getPrivileges(): JSONArray? {
        val json = context.dataStore.data.first()[PRIVILEGE_KEY]
        return json?.let { JSONArray(it) }
    }

    // ✅ Clear all session
    suspend fun clearSession() {
        context.dataStore.edit {
            it.clear()
        }
    }
}