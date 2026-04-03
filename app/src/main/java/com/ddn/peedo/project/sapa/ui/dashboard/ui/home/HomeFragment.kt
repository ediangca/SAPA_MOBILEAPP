package com.ddn.peedo.project.sapa.ui.dashboard.ui.home

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.databinding.FragmentHomeBinding
import com.ddn.peedo.project.sapa.model.User
import com.ddn.peedo.project.sapa.store.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val session by lazy {
        SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initComponent()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initComponent() {
        lifecycleScope.launch {

            val userJson = session.getUser()
            val privs = session.getPrivileges()

            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern(
                "EEEE, MMM dd, yyyy",
                Locale.ENGLISH
            )

            if (userJson == null) {
                Log.e("SESSION", "User not found → redirect to login")
                return@launch
            }

            val userId = userJson.getString("userID")
            val lastname = userJson.getString("lastname")
            val fullname = userJson.getString("fullname")
            val roleId = userJson.getString("roleID")

            Log.d("USER_DATA", "ID: $userId")
            Log.d("USER_DATA", "Name: $fullname")
            Log.d("USER_DATA", "Role: $roleId")

            val user = Gson().fromJson(userJson.toString(), User::class.java)

            with(binding) {
                dateText.text = today.format(formatter)
                userDisplayName.text = "Hi ${user.firstname}, Good day!"
            }

            Log.d("SESSION", "User: $user")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}