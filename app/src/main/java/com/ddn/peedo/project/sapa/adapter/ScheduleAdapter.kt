package com.ddn.peedo.project.sapa.adapter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.databinding.ItemScheduleBinding
import com.ddn.peedo.project.sapa.dataclass.HospitalScheduleUi
import com.ddn.peedo.project.sapa.model.VwSlot
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ddn.peedo.project.sapa.R
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Visibility
import com.ddn.peedo.project.sapa.databinding.DialogDepartmentShiftsBinding
import com.ddn.peedo.project.sapa.databinding.DialogScanQrBinding
import com.ddn.peedo.project.sapa.databinding.DialogStudentsBinding
import com.ddn.peedo.project.sapa.dataclass.ApiErrorResponse
import com.ddn.peedo.project.sapa.dataclass.AttendanceRequest
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.services.QRCodeAnalyzer
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class ScheduleAdapter(
    private var items: List<HospitalScheduleUi>,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onShiftClick: (VwSlot) -> Unit,
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private var qrDialog: Dialog? = null
    private var qrBinding: DialogScanQrBinding? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isScanning = false
    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0
    val dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a") // 👈 12-hour format

    private val session by lazy {
        SessionManager(context)
    }
    private  lateinit var user: VwUser

    inner class ScheduleViewHolder(
        val binding: ItemScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ScheduleViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = items[position]
        val first = item.slots.firstOrNull() ?: return

        // =========================
        // HEADER
        // =========================
        holder.binding.dateSlot.text = item.date
        holder.binding.tvSchoolName.text = item.schoolName

        holder.binding.tvHospitalName.text = item.hospitalName ?: ""

        // =========================
        // DEPARTMENT CHIPS
        // =========================
        holder.binding.shiftChipContainer.removeAllViews()

        item.slots.groupBy { it.sectionName }
            .forEach { (department, departmentSlots) ->

                val chip = LayoutInflater.from(holder.itemView.context).inflate(
                    R.layout.item_shift_chip, holder.binding.shiftChipContainer, false
                ) as TextView

                chip.text = department ?: "Department"

                chip.setOnClickListener {
                    showDepartmentDialog(
                        holder.itemView.context,
                        item.schoolName,
                        item.hospitalName,
                        department,
                        departmentSlots
                    )
                }

                holder.binding.shiftChipContainer.addView(chip)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun showDepartmentDialog(
        context: Context,
        school: String?,
        hospital: String?,
        department: String?,
        slots: List<VwSlot>
    ) {
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        soundId = soundPool.load(context, R.raw.beep, 1)

        val dialog = Dialog(context, R.style.BottomDialogStyle)
        val bindingDialogDepartmentShifts =
            DialogDepartmentShiftsBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(bindingDialogDepartmentShifts.root)
        dialog.setCancelable(true)

        val schoolTitle = dialog.findViewById<TextView>(R.id.txtSchoolTitle)
        val departmentTitle = dialog.findViewById<TextView>(R.id.txtDepartmentTitle)
        val container = dialog.findViewById<LinearLayout>(R.id.shiftListContainer)
        val btnClose = dialog.findViewById<TextView>(R.id.btnClose)

        schoolTitle.text = school ?: "School"
        (if (hospital != null && department != null) "$hospital - $department" else "Department").also {
            departmentTitle.text = it
        }


        slots.groupBy { it.slotID }.forEach { (_, slotGroup) ->
            CoroutineScope(Dispatchers.Main).launch {
                try {

                    val slot = slotGroup.first()

                    val res = RetrofitClient
                        .create(context)
                        .getAppointedStudentsBySlotID(slot.slotID)

                    if (!res.isSuccessful) {
                        Log.d("ScheduleFragment_INFO", "Error loading students")
                        return@launch
                    }
                    val appointedStudent = res.body() ?: emptyList()

                    val row =
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_shift_row, container, false)


                    val txtShift = row.findViewById<TextView>(R.id.txtShiftName)
                    val txtShiftTime = row.findViewById<TextView>(R.id.txtShiftTime)
                    val txtAlloc = row.findViewById<TextView>(R.id.txtAllocation)
                    val qrScan = row.findViewById<AppCompatImageButton>(R.id.scanQRAttendance)


                    val allocated = appointedStudent.size
                    val capacity = slot.allocation ?: 0

                    txtShift.text = slot.shiftName
                    txtShiftTime.text = "${to12Hour(slot.startTime)} - ${to12Hour(slot.endTime)}"
                    txtAlloc.text = if (capacity > 0) "$allocated / $capacity" else "0"

//                    val isFull = capacity > 0 && allocated >= capacity
//                    row.alpha = if (isFull) 0.4f else 1f
//                    row.isEnabled = !isFull

                    val userJson = session.getUser()

                    user = Gson().fromJson(userJson.toString(), VwUser::class.java)

                    qrScan.visibility = when (user.roleID) {
                        "UGR0001", "UGR0002" -> {
                            View.VISIBLE
                        }
                        else -> {
                            View.GONE
                        }
                    }

                    qrScan.setOnClickListener {
                        if (!isWithinSlotTime(slot.startTime, slot.endTime)) {

                            val startTime = LocalTime.parse(slot.startTime, dateFormatter)
                            val endTime = LocalTime.parse(slot.endTime, dateFormatter)


                            val adjustedStart = startTime.minusHours(1)   // 1 hour before
                            val adjustedEnd = startTime.plusHours(1)      // 1 hour after


                            SweetAlertUtil.showWarning(
                                context,
                                "Not Allowed",
                                "QR scanning is only allowed between\n" +
                                        "${adjustedStart.format(timeFormatter)} - ${adjustedEnd.format(timeFormatter)}"
                            )

                            return@setOnClickListener
                        }
                        onQrScanClicked(slot)
                    }

                    row.setOnClickListener {
//                        if (!isFull) {
//                    dialog.dismiss()
                        onShiftClick(slot)
                        showStudentsDialog(slot)
//                        }
                    }

                    container.addView(row)

                } catch (e: Exception) {
                    Log.e("ScheduleFragment_INFO", "Error loading appointed Student", e)
                    Toast.makeText(context, "Error loading appointed students", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
        val sheet = dialog.findViewById<View>(R.id.sheetContainer)

        var startY = 0f

        sheet.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val delta = event.rawY - startY
                    if (delta > 0) {
                        v.translationY = delta
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (v.translationY > v.height / 4) {
                        dialog.dismiss()
                    } else {
                        v.animate().translationY(0f).setDuration(200).start()
                    }
                    true
                }

                else -> false
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))


        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isWithinSlotTime(start: String?, end: String?): Boolean {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return false

        return try {

            val startTime = LocalTime.parse(start, dateFormatter)
            val endTime = LocalTime.parse(end, dateFormatter)

            // ✅ Adjusted window
            val adjustedStart = startTime.minusHours(1)   // 1 hour before
            val adjustedEnd = startTime.plusHours(1)      // 1 hour after


            val now = LocalTime.now()

            Log.d("ScheduleFragment_INFO", "TIME_CHECK Now: $now | Start: $startTime | End: $endTime")

            Log.d("ScheduleFragment_INFO", "TIME_CHECK 1hr allowance Now: $now | Start: $adjustedStart | End: $adjustedEnd")
//            now.isAfter(startTime) && now.isBefore(endTime)
//            if (startTime <= endTime) {
            // ✅ NORMAL CASE (same day)
//            now >= adjustedStart && now <= adjustedEnd

            if (adjustedStart <= adjustedEnd) {
                now >= startTime && now <= endTime
//                now.isAfter(adjustedStart) && now.isBefore(adjustedEnd)
            } else {
                // 🌙 OVERNIGHT CASE (crosses midnight)
//                now >= startTime || now <= endTime
                now >= adjustedStart || now <= adjustedEnd
//                now.isAfter(adjustedStart) && now.isBefore(adjustedEnd)
            }

        } catch (e: Exception) {
            false
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showStudentsDialog(slot: VwSlot) {

        Log.d("ScheduleFragment_INFO", "Showing Students Dialog")

        val binding = DialogStudentsBinding.inflate(LayoutInflater.from(context))

        binding.txtTitle.text = "List of assigned Intern(s)"

        val dialog = Dialog(context).apply {
            setContentView(binding.root)
            setCancelable(true)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.92).toInt(), // 92% width
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val adapter = StudentAdapter(emptyList(), context, lifecycleOwner)
        binding.studentRecycler.layoutManager = LinearLayoutManager(context)
        binding.studentRecycler.adapter = adapter

        // 👇 IMPORTANT
        showLoading(binding)
        dialog.show()   // ✅ SHOW FIRST

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val res = RetrofitClient
                    .create(context)
                    .getAppointedStudentsBySlotID(slot.slotID)

                if (res.isSuccessful) {
                    val appointedStudent = res.body() ?: emptyList()

                    if (appointedStudent.isEmpty()) {
                        Log.d("ScheduleFragment_INFO", "No interns assigned")
                        showEmpty(binding)
                    } else {
                        Log.d("ScheduleFragment_INFO", "Updating Data: ${appointedStudent.size}")
                        adapter.updateData(appointedStudent)
                        showList(binding)
                    }
                } else {
                    showEmpty(binding)
                }

            } catch (e: Exception) {
                Log.e("ScheduleFragment_INFO", "Error loading appointed Student", e)
                showEmpty(binding)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun to12Hour(time: String?): String {
        if (time.isNullOrBlank()) return ""

        return try {
            val input = DateTimeFormatter.ofPattern("HH:mm:ss")
            val output = DateTimeFormatter.ofPattern("hh:mm a")

            LocalTime.parse(time, input).format(output)
        } catch (e: Exception) {
            time // fallback (won’t crash)
        }
    }

    private fun showLoading(binding: DialogStudentsBinding) {
        binding.loadingState.visibility = View.VISIBLE
        binding.studentRecycler.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    private fun showEmpty(binding: DialogStudentsBinding) {
        binding.loadingState.visibility = View.GONE
        binding.studentRecycler.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE

        binding.emptyState.apply {
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).start()
        }
    }

    private fun showList(binding: DialogStudentsBinding) {
        binding.studentRecycler.visibility = View.VISIBLE
        binding.loadingState.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<HospitalScheduleUi>) {
        items = newList
        notifyDataSetChanged()
    }


    private fun onQrScanClicked(slot: VwSlot) {

        if (qrDialog?.isShowing == true) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            )
        } else {
            showScanQrDialog(slot)
        }
    }

    private fun showScanQrDialog(slot: VwSlot) {

        if (qrDialog?.isShowing == true) return

        qrBinding = DialogScanQrBinding.inflate(LayoutInflater.from(context))

        qrDialog = Dialog(context).apply {
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
                SweetAlertUtil.showWarning(context, "QR Code", text)
            }
        }

        qrDialog!!.show()
        startCamera(slot)
    }

    private fun startCamera(slot: VwSlot) {

        val providerFuture =
            ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({

            cameraProvider = providerFuture.get()
            cameraProvider?.unbindAll()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(qrBinding!!.previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {

                    setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        QRCodeAnalyzer { qr ->

                            Handler(Looper.getMainLooper()).post {

                                if (isScanning || qr.isEmpty()) return@post
                                isScanning = true

                                soundPool.play(
                                    soundId,
                                    1f,
                                    1f,
                                    0,
                                    0,
                                    1f
                                )

                                val scannedUserId = qr.trim()
                                val slotId = slot.slotID

                                Log.d(
                                    "ScheduleFragment_INFO",
                                    "Scan QR: $scannedUserId to slot $slotId"
                                )

                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        // 1️⃣ Validate attendance
                                        Toast.makeText(context, "Scanning...", Toast.LENGTH_SHORT)
                                            .show()
                                        val validateResponse = RetrofitClient.create(context)
                                            .validateAttendance(scannedUserId, slotId)

                                        Log.d(
                                            "ScheduleFragment_INFO",
                                            "Scan QR: ${validateResponse.isSuccessful}"
                                        )
                                        if (!validateResponse.isSuccessful || validateResponse.body() == null) {
                                            SweetAlertUtil.showError(
                                                context,
                                                "Error",
                                                "Unable to validate attendance."
                                            )
                                            return@launch
                                        }

                                        val validation = validateResponse.body()!!

                                        // 2️⃣ Attendance already exists
                                        if (validation.hasAttendance) {

                                            SweetAlertUtil.showWarning(
                                                context,
                                                "Already Recorded",
                                                "This intern already has attendance for this slot."
                                            )
                                            return@launch
                                        }

                                        // 3️⃣ Ask confirmation before posting attendance
                                        SweetAlertUtil.showConfirm(
                                            context,
                                            "Confirm Attendance",
                                            "Do you want to record attendance for this intern?",
                                            confirmText = "Yes",
                                            cancelText = "No"
                                        ) {

                                            val attendanceRequest =
                                                AttendanceRequest(slotId, scannedUserId)
                                            Log.d(
                                                "ScheduleFragment_INFO",
                                                "Saving ... $attendanceRequest"
                                            )
                                            CoroutineScope(Dispatchers.Main).launch {
                                                // 4️⃣ POST attendance
                                                try {
                                                    val postResponse =
                                                        RetrofitClient.create(context)
                                                            .postAttendance(attendanceRequest)

                                                    if (postResponse.isSuccessful) {

                                                        SweetAlertUtil.showSuccess(
                                                            context,
                                                            "Success",
                                                            "Attendance recorded successfully."
                                                        )

                                                    } else {

                                                        val errorBody =
                                                            postResponse.errorBody()?.string()
                                                        val gson = Gson()

                                                        val apiError = try {
                                                            gson.fromJson(
                                                                errorBody,
                                                                ApiErrorResponse::class.java
                                                            )
                                                        } catch (e: Exception) {
                                                            null
                                                        }

                                                        when (postResponse.code()) {

                                                            404 -> {
                                                                SweetAlertUtil.showWarning(
                                                                    context,
                                                                    "Not Found",
                                                                    apiError?.message
                                                                        ?: "No record found."
                                                                )
                                                            }

                                                            400 -> {
                                                                SweetAlertUtil.showWarning(
                                                                    context,
                                                                    "Not Allowed",
                                                                    apiError?.message
                                                                        ?: "Bad request."
                                                                )
                                                            }

                                                            400 -> {
                                                                SweetAlertUtil.showWarning(
                                                                    context,
                                                                    "Not Allowed",
                                                                    apiError?.message
                                                                        ?: "Bad request."
                                                                )
                                                            }

                                                            409 -> {
                                                                SweetAlertUtil.showWarning(
                                                                    context,
                                                                    "Duplicate",
                                                                    apiError?.message
                                                                        ?: "Attendance already exists."
                                                                )
                                                            }

                                                            else -> {
                                                                SweetAlertUtil.showError(
                                                                    context,
                                                                    "Error ${postResponse.code()}",
                                                                    apiError?.message
                                                                        ?: "Something went wrong."
                                                                )
                                                            }
                                                        }
                                                    }

                                                } catch (e: Exception) {
                                                    Log.d(
                                                        "ScheduleFragment_INFO",
                                                        "Error validating attendance",
                                                        e
                                                    )
                                                    SweetAlertUtil.showError(
                                                        context,
                                                        "Network Error",
                                                        e.localizedMessage ?: "Something went wrong"
                                                    )
                                                }
                                            }
                                        }


                                    } catch (e: Exception) {
                                        Log.d(
                                            "ScheduleFragment_INFO",
                                            "Error validating attendance",
                                            e
                                        )
                                        SweetAlertUtil.showError(
                                            context,
                                            "Network Error",
                                            e.localizedMessage ?: "Something went wrong"
                                        )
                                    } finally {
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            isScanning = false
                                        }, 3000)
                                    }
                                }
//                                Handler(Looper.getMainLooper()).postDelayed({
//                                    isScanning = false
//                                }, 3000)
                            }
                        }
                    )
                }

            cameraProvider?.bindToLifecycle(
                lifecycleOwner, // ✅ CORRECT
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analyzer
            )

        }, ContextCompat.getMainExecutor(context))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

}
