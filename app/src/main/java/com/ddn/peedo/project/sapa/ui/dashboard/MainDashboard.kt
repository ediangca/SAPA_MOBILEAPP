package com.ddn.peedo.project.sapa.ui.dashboard

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.adapter.RecentScheduleAdapter
import com.ddn.peedo.project.sapa.databinding.ActivityMainDashboardBinding
import com.ddn.peedo.project.sapa.databinding.DialogScanQrBinding
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.services.QRCodeAnalyzer
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.ui.dashboard.ui.home.HomeFragment
import com.ddn.peedo.project.sapa.ui.dashboard.ui.profile.ProfileFragment
import com.ddn.peedo.project.sapa.ui.dashboard.ui.reports.ReportsFragment
import com.ddn.peedo.project.sapa.ui.dashboard.ui.schedule.ScheduleFragment
import com.ddn.peedo.project.sapa.ui.dashboard.ui.school.SchoolFragment
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class MainDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityMainDashboardBinding
    private var bundle = Bundle()
    private lateinit var mainFrame: FragmentTransaction
    private lateinit var homeFragment: HomeFragment
    private lateinit var schoolFragment: SchoolFragment
    private lateinit var scheduleFragment: ScheduleFragment
    private lateinit var reportFragment: ReportsFragment
    private lateinit var profileFragment: ProfileFragment
    private var qrDialog: Dialog? = null
    private var qrBinding: DialogScanQrBinding? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isScanning = false
    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onInit()


        val session = SessionManager(this)

        lifecycleScope.launch {
            val userJson = session.getUser()

            if (userJson != null) {
                val user = Gson().fromJson(userJson.toString(), VwUser::class.java)

                navAccess(user)
            }
        }


    }

    private fun onInit() {

        homeFragment = HomeFragment()
        schoolFragment = SchoolFragment()
        scheduleFragment = ScheduleFragment()
        reportFragment = ReportsFragment()
        profileFragment = ProfileFragment()


        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        soundId = soundPool.load(this, R.raw.beep, 1)

        qrBinding = DialogScanQrBinding.inflate(this.layoutInflater)

        with(binding) {

            binding.mainNav.setOnItemSelectedListener { item ->

                val view = binding.mainNav.findViewById<View>(item.itemId)
                view?.animate()?.scaleX(1.15f)?.scaleY(1.15f)?.setDuration(150)?.start()
                view?.animate()
                    ?.scaleX(1.1f)
                    ?.scaleY(1.1f)
                    ?.setDuration(120)
                    ?.withEndAction {
                        view.scaleX = 1f
                        view.scaleY = 1f
                    }
                    ?.start()
                true

                navMenuOnItemSelectedListener(item)
            }


        }
    }


    fun navAccess(user: VwUser) {
        val menu = binding.mainNav.menu

        // Reset all (important if reused)
        menu.findItem(R.id.navigation_home)?.isVisible = true
        menu.findItem(R.id.navigation_schedule)?.isVisible = true
        menu.findItem(R.id.navigation_profile)?.isVisible = true
        menu.findItem(R.id.navigation_school)?.isVisible = true
        menu.findItem(R.id.navigation_report)?.isVisible = true

        when (user.roleID) {
            "UGR0001", "UGR0002" -> {
                binding.mainNav.menu.findItem(R.id.navigation_report)?.isVisible = false
            }

            "UGR0003" -> {
                binding.mainNav.menu.findItem(R.id.navigation_school)?.isVisible = false
                binding.mainNav.menu.findItem(R.id.navigation_report)?.isVisible = false
            }

            "UGR0004" -> {
                binding.mainNav.menu.findItem(R.id.navigation_school)?.isVisible = false
                binding.mainNav.menu.findItem(R.id.navigation_report)?.isVisible = false
            }

            "UGR0005" -> {
                binding.mainNav.menu.findItem(R.id.navigation_school)?.isVisible = false
                binding.mainNav.menu.findItem(R.id.navigation_report)?.isVisible = false
            }
        }
    }


    private fun onQrScanClicked() {

        if (qrDialog?.isShowing == true) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        } else {
            showScanQrDialog()
        }
    }


    private fun MainDashboard.onQrScanClicked() {
        Log.d("MainActivity_INFO", "ON PERMISSION")
        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onRequestPermissionsResult(
                101,
                arrayOf(Manifest.permission.CAMERA),
                intArrayOf(PackageManager.PERMISSION_GRANTED)
            )
        } else {
            // Initialize camera if permission is already granted
            Log.d("MainActivity_INFO", "Initializing camera")
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            showScanQrDialog()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {

        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({

            cameraProvider = providerFuture.get()
            cameraProvider?.unbindAll()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(qrBinding!!.previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder().build().apply {
                setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    QRCodeAnalyzer { qr ->
                        runOnUiThread {
                            if (isScanning || qr.isEmpty()) return@runOnUiThread
                            isScanning = true

                            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)

                            SweetAlertUtil.showWarning(this@MainDashboard, "Scanned", qr)

                            Handler(Looper.getMainLooper()).postDelayed({
                                isScanning = false
                            }, 3000)
                        }
                    }
                )
            }

            cameraProvider?.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analyzer
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun navMenuOnItemSelectedListener(it: MenuItem?): Boolean {
//        bundle = Bundle()
//        bundle.putParcelable("user", user)
//        homeFragment.arguments = bundle
        if (it == null) {
            mainFrame = supportFragmentManager.beginTransaction()
            mainFrame.replace(R.id.main_fragment, homeFragment);
            mainFrame.addToBackStack(null);
            mainFrame.commit();
            return true
        } else {
            Log.d("MainActivity_INFO", "MENU ITEM ID: ${it.itemId}  --------------")
            bundle = Bundle()
            when (it.itemId) {
                R.id.navigation_home -> {
//                    homeFragment.arguments = bundle
                    mainFrame = supportFragmentManager.beginTransaction()
                    mainFrame.replace(R.id.main_fragment, homeFragment);
                    mainFrame.addToBackStack(null);
                    mainFrame.commit();
                    return true
                }

                R.id.navigation_school -> {
//                    homeFragment.arguments = bundle
                    mainFrame = supportFragmentManager.beginTransaction()
                    mainFrame.replace(R.id.main_fragment, schoolFragment);
                    mainFrame.addToBackStack(null);
                    mainFrame.commit();
                    return true
                }

                R.id.navigation_schedule -> {
//                    homeFragment.arguments = bundle
                    mainFrame = supportFragmentManager.beginTransaction()
                    mainFrame.replace(R.id.main_fragment, scheduleFragment);
                    mainFrame.addToBackStack(null);
                    mainFrame.commit();
                    return true
                }

                R.id.navigation_report -> {
//                    homeFragment.arguments = bundle
                    mainFrame = supportFragmentManager.beginTransaction()
                    mainFrame.replace(R.id.main_fragment, reportFragment);
                    mainFrame.addToBackStack(null);
                    mainFrame.commit();
                    return true
                }

                else -> {
//                    homeFragment.arguments = bundle
                    mainFrame = supportFragmentManager.beginTransaction()
                    mainFrame.replace(R.id.main_fragment, profileFragment);
                    mainFrame.addToBackStack(null);
                    mainFrame.commit();
                    return true
                }
            }
        }
    }

    private fun showScanQrDialog() {

        if (qrDialog?.isShowing == true) return

        qrBinding = DialogScanQrBinding.inflate(layoutInflater)

        qrDialog = Dialog(this).apply {
            setContentView(qrBinding!!.root)
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.92).toInt(), // 92% width
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        qrBinding!!.btnClose.setOnClickListener {
            stopCamera()
            qrDialog?.dismiss()
            qrDialog = null
            qrBinding = null
        }

        qrBinding!!.fetchButton.setOnClickListener {
            val text = qrBinding!!.filter.text.toString().trim()
            if (text.isEmpty()) {
                qrBinding!!.filter.error = "Please enter QR Code"
            } else {
                SweetAlertUtil.showWarning(this, "QR Code", text)
            }
        }

        qrDialog!!.show()
        startCamera()   // ✅ camera AFTER dialog
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }


}

