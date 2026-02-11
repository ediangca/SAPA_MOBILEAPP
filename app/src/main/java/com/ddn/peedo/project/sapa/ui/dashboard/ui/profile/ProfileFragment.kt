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
import com.ddn.peedo.project.sapa.store.TokenManager
import com.ddn.peedo.project.sapa.ui.dashboard.MainDashboard
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

        val tokenManager = TokenManager(requireContext())

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation(tokenManager)
        }
    }

    private fun showLogoutConfirmation(tokenManager: TokenManager) {
        SweetAlertUtil.showConfirm(
            requireContext(),
            "Sign Out",
            "Are you sure you want to logout?",
            confirmText = "Yes, Logout",
            cancelText = "Cancel"
        ) {
            performLogout(tokenManager)
        }
    }

    private fun performLogout(tokenManager: TokenManager) {
        lifecycleScope.launch {
            try {
                // 1️⃣ Clear token
                tokenManager.clearToken()

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