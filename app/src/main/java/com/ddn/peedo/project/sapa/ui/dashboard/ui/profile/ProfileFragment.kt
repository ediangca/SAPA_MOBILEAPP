package com.ddn.peedo.project.sapa.ui.dashboard.ui.profile

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.LoginActivity
import com.ddn.peedo.project.sapa.databinding.FragmentProfileBinding
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.repository.OfflineSyncManager
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView

import com.ddn.peedo.project.sapa.R

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

            initOfflineDataCard()

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

            val user = Gson().fromJson(userJson.toString(), VwUser::class.java)


            Log.d("USER_DATA", "User: $user")

            with(binding) {

                userDisplayName.text = user.username
                userRole.text = user.rolename
                userID.text = user.userID
                fullname.text = user.fullname

                if (user.schoolID != null) {
                    schoolName.text = user.schoolName
                }else{
                    schoolLabel.visibility = View.GONE
                    schoolName.visibility = View.GONE
                }

                if (user.hospitalID != null){
                    hospitalName.text = user.hospitalName
                }else{
                    hospitalLabel.visibility = View.GONE
                    hospitalName.visibility = View.GONE
                }


                btnLogout.setOnClickListener {
                    showLogoutConfirmation(session)
                }


                if (user.roleID == "UGR0001" || user.roleID == "UGR0002") {
                    offlineSchedule.visibility = View.VISIBLE
                    offlineInterns.visibility = View.VISIBLE
                    offlineAttendance.visibility = View.VISIBLE
                    offlineSchool.visibility = View.VISIBLE
                    offlineUsersDevice.visibility = View.VISIBLE
                    offlineUpload.visibility = View.VISIBLE
                }else if(user.roleID == "UGR0003" || user.roleID == "UGR0006"){
                    offlineSchedule.visibility = View.VISIBLE
                    offlineInterns.visibility = View.VISIBLE
                    offlineAttendance.visibility = View.VISIBLE
                }else if(user.roleID == "UGR0004"){
                    offlineInterns.visibility = View.VISIBLE
                }else if(user.roleID == "UGR0005"){
                    offlineSchedule.visibility = View.VISIBLE
                    offlineInterns.visibility = View.VISIBLE
                    offlineAttendance.visibility = View.VISIBLE
                    offlineUpload.visibility = View.VISIBLE
                }

                if (user.roleID == "UGR0001" || user.roleID == "UGR0004" || user.roleID == "UGR0006") {

                    val qrBitmap = generateQr(user.userID, 300)
                    profilePicture.setImageBitmap(qrBitmap)

                    profilePicture.setOnClickListener {
                        showQrDialog(user.userID)
                    }
                }
            }

            Log.d("SESSION", "User: $user")
        }
    }

    // =========================================================
    // OFFLINE DATA CARD
    //
    // Shows what the replication has stored on this device (last sync
    // time + record counts + pending uploads) and offers a manual
    // full re-download.
    // =========================================================

    private fun initOfflineDataCard() {
        val db = SapaDatabase.getInstance(requireContext())

        lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                OfflineDataCounts(
                    schedules = db.slotDao().getAllOnce().size,
                    interns = db.appointedStudentDao().getAllOnce().size,
                    attendance = db.attendanceDao().getAllOnce().size,
                    schools = db.schoolDao().getAllOnce().size,
                    users = db.userDao().getAllOnce().size,
                    pendingUploads = db.attendanceQueueDao().pendingCount()
                )
            }

            val lastSync = OfflineSyncManager.lastSyncTime(requireContext())

            binding.txtLastSync.text = if (lastSync > 0) {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.ENGLISH)
                "Last sync: ${sdf.format(java.util.Date(lastSync))}"
            } else {
                "Last sync: never"
            }

            binding.countSchedules.text = counts.schedules.toString()
            binding.countInterns.text = counts.interns.toString()
            binding.countAttendance.text = counts.attendance.toString()
            binding.countSchools.text = counts.schools.toString()
            binding.countUsers.text = counts.users.toString()
            binding.countPendingUploads.text = counts.pendingUploads.toString()
        }

        binding.btnRedownload.setOnClickListener {
            SweetAlertUtil.showConfirm(
                requireContext(),
                "Re-download Data",
                "Download a fresh copy of schedules, interns, attendance, " +
                        "schools and users from the server?\n\nOffline records waiting " +
                        "to upload are never lost.",
                confirmText = "Download",
                cancelText = "Cancel"
            ) {
                OfflineSyncManager.startAutoSync(requireContext(), force = true)
            }
        }
    }

    private data class OfflineDataCounts(
        val schedules: Int,
        val interns: Int,
        val attendance: Int,
        val schools: Int,
        val users: Int,
        val pendingUploads: Int
    )

    fun generateQr(content: String, size: Int = 300): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }
        return bmp
    }

    fun showQrDialog(userId: String) {

        val dialog = Dialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_qr, null)

        val img = view.findViewById<ImageView>(R.id.imgQrLarge)

        img.setImageBitmap(generateQr(userId, 600))

        dialog.setContentView(view)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
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