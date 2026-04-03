package com.ddn.peedo.project.sapa.ui.dashboard.ui.profile

import android.content.Intent
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.LoginActivity
import com.ddn.peedo.project.sapa.databinding.FragmentProfileBinding
import com.ddn.peedo.project.sapa.model.User
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.ui.dashboard.MainDashboard
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

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
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        Log.d(
            "ScheduleFragment_INFO",
            "Binding: "
        )
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

//        val textView: TextView = binding.textConstruction
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
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
            val roleId = userJson.getString("roleID")

            Log.d("USER_DATA", "ID: $userId")
            Log.d("USER_DATA", "Role: $roleId")

            val user = Gson().fromJson(userJson.toString(), User::class.java)

            with(binding) {

                userDisplayName.text = user.username
                userRole.text = user.rolename
                userID.text = user.userID
                fullname.text = user.fullname


                btnLogout.setOnClickListener {
                    showLogoutConfirmation(session)
                }
            }

            Log.d("SESSION", "User: $user")
        }
    }

    private fun showLogoutConfirmation(session: SessionManager) {
        SweetAlertUtil.showConfirm(
            requireContext(),
            "Sign Out",
            "Are you sure you want to logout?",
            confirmText = "Yes, Logout",
            cancelText = "Cancel"
        ) {
            performLogout(session)
        }
    }

    private fun performLogout(session: SessionManager) {
        lifecycleScope.launch {
            try {
                // 1️⃣ Clear token
                session.clearSession()

                // 2️⃣ Navigate to Login
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)

            } catch (e: Exception) {
                SweetAlertUtil.showError(
                    requireContext(),
                    "Logout Failed",
                    "Unable to logout. Please try again."
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}