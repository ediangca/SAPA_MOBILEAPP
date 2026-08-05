package com.ddn.peedo.project.sapa.ui.dashboard.ui.home

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.adapter.ScheduleAdapter
import com.ddn.peedo.project.sapa.databinding.DialogTodayScheduleBinding
import com.ddn.peedo.project.sapa.databinding.FragmentHomeBinding
import com.ddn.peedo.project.sapa.dataclass.HospitalScheduleUi
import com.ddn.peedo.project.sapa.model.DashboardSummary
import com.ddn.peedo.project.sapa.model.VwSlot
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.util.UserRoleUtil
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val session by lazy {
        SessionManager(requireContext())
    }

    private  lateinit var user: VwUser

    private var slots: List<VwSlot> = emptyList()
    private val list = ArrayList<VwSlot>()

    private var recentSchedules: List<VwSlot> = emptyList()
    private var todaySlotsCache: List<VwSlot> = emptyList()

    private lateinit var scheduleadapter: ScheduleAdapter

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.ENGLISH)
    private var isSheetExpanded = false
    private var collapsedSheetHeight = 0

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
        initRecycler()
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

            Log.d("HomeFragment_INFO", "USER DATA ID: $userId")
            Log.d("HomeFragment_INFO", "USER DATA Name: $fullname")
            Log.d("HomeFragment_INFO", "USER DATA Role: $roleId")

            user = Gson().fromJson(userJson.toString(), VwUser::class.java)

            with(binding) {
                dateText.text = today.format(formatter)
                userDisplayName.text = "Hi ${user.firstname}, Good day!"

                swipeRefresh.setOnRefreshListener {
                    loadRecentSchedule(user)
                }

                todayScheduleCard.setOnClickListener {
                    showTodayScheduleDialog()
                }

            }

            Log.d("HomeFragment_INFO", "SESSION User: $user")

            loadDashboard(user)
        }

    }

    private fun initRecycler() {
        scheduleadapter = ScheduleAdapter(
            emptyList(), requireContext(),
            lifecycleOwner = viewLifecycleOwner
        ) { slot ->
            // TODO: handle shift click
            // slot.slotID
            // slot.shiftName
            // slot.hospitalID
            // slot.schoolID
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = scheduleadapter



        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = scheduleadapter
//        binding.list.isNestedScrollingEnabled = false   // <-- add this line

        binding.list.post {
            Log.d("HomeFragment_INFO", "RecyclerView height: ${binding.list.height}, item count: ${scheduleadapter.itemCount}")
        }
    }

    fun loadDashboard(user: VwUser) {

        lifecycleScope.launch {
            try {

                Log.d("HomeFragment_INFO", "Loading Dashboard: ${user.userID} - ${user.roleID}")
                val response = RetrofitClient.api(requireContext())
                    .getDashboardSummary(user.userID, user.roleID)

                if (response.isSuccessful && response.body() != null) {

                    val data = response.body()!!

                    Log.d("HomeFragment_INFO", "Loaded: ${Gson().toJson(data)}")

                    bindDashboard(data, user)

                }

            } catch (e: Exception) {
                Log.e("HomeFragment_INFO", "Error: ${e.message}")
            }
        }
    }

    fun bindDashboard(data: DashboardSummary, user: VwUser) {

        with(binding) {

            pendingCount.text = data.pendingSchedule.toString()
            confirmCount.text = data.confirmedSchedule.toString()
            studentCount.text = data.totalAppointedStudents.toString()

            when (user.roleID) {

                // ADMIN / SUPERVISOR
                "UGR0001", "UGR0002" -> {
                    cardPending.visibility = View.VISIBLE
                    cardConfirmed.visibility = View.VISIBLE
                    cardStudents.visibility = View.VISIBLE
                    cardAttendance.visibility = View.GONE
                    cardFuture.visibility = View.GONE
                }

                // SCHOOL COORDINATOR / SUPERVISOR / CI
                "UGR0003", "UGR0005", "UGR0006"  -> {
                    cardPending.visibility = View.VISIBLE
                    cardConfirmed.visibility = View.VISIBLE
                    cardStudents.visibility = View.VISIBLE
                    cardAttendance.visibility = View.GONE
                    cardFuture.visibility = View.GONE
                }

                // STUDENT
                "UGR0004" -> {
                    attendanceCount.text = data.totalAttendances.toString()
                    futureCount.text = data.futureSlots.toString()
                    cardPending.visibility = View.GONE
                    cardConfirmed.visibility = View.VISIBLE
                    cardStudents.visibility = View.GONE
                    cardAttendance.visibility = View.VISIBLE
                    cardFuture.visibility = View.VISIBLE
                }
            }


            // NEW: gate + populate analytics
            val canSeeAnalytics = UserRoleUtil.canViewAnalytics(user.roleID)
            analyticsSection.visibility = if (canSeeAnalytics) View.VISIBLE else View.GONE
            if (canSeeAnalytics) {
                bindStatusBreakdownChart(data)
            }
        }

        loadRecentSchedule(user)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadRecentSchedule(user: VwUser) {

        showLoading()

        lifecycleScope.launch {

            try {

                val api = RetrofitClient.api(requireContext())
                val year = LocalDate.now().year

                Log.d("HomeFragment_INFO", "Fetching slots for role: ${user.roleID}")

                val response = when (user.roleID) {

                    "UGR0001", "UGR0002" -> {
                        Log.d("HomeFragment_INFO", "Admin / Supervisor")
                        api.getSlots(year)
                    }

                    "UGR0003" -> {
                        Log.d("HomeFragment_INFO", "Student")
                        api.getSlotsByUserID(user.userID, year)
                    }

                    "UGR0004" -> {
                        Log.d("HomeFragment_INFO", "Appointed User")
                        api.getSlotsByAppointUserID(user.userID, year)
                    }

                    "UGR0005" -> {
                        Log.d("HomeFragment_INFO", "Hospital")
                        api.getSlotsByHospitalID(user.hospitalID ?: "", year)
                    }

                    "UGR0006" -> {
                        Log.d("HomeFragment_INFO", "CI User")
                        api.getSlotsByCI(user.userID ?: "", year)
                    }

                    else -> {
                        Log.e("HomeFragment_INFO", "Unknown role")
                        return@launch
                    }
                }

                hideLoading()
                binding.swipeRefresh.isRefreshing = false
                list.clear()

                if (response.isSuccessful && response.body() != null) {
                    processSlots(response.body()!!)
                } else {
                    Log.e("HomeFragment_INFO", "Failed to fetch slots")
                }

            } catch (e: Exception) {
                hideLoading()
                showNoInternetState()
                Log.e("HomeFragment_INFO", "Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processSlots(data: List<VwSlot>) {

        if (data.isEmpty()) {
            updateEmptyState(emptyList())
            updateTodayScheduleCard(emptyList())
            return
        }

        Log.d("HomeFragment_INFO", "All Slots: ${Gson().toJson(data)}")

        // ✅ Filter active slots
        slots = data.filter { it.slotStatus == 1 }

        // ✅ Sort by date DESC, most recent first (original behavior)
        recentSchedules = slots
            .sortedByDescending {
                try {
                    LocalDate.parse(it.dateSlot)
                } catch (e: Exception) {
                    LocalDate.MIN
                }
            }
            .take(10)

        val grouped = groupBySchoolHospital(recentSchedules)
        scheduleadapter.updateData(grouped)
        updateEmptyState(recentSchedules)

        // ✅ Today's slots computed separately, purely for the banner + dialog
        val today = LocalDate.now()
        val todaySlots = slots.filter {
            try {
                LocalDate.parse(it.dateSlot) == today
            } catch (e: Exception) {
                false
            }
        }
        updateTodayScheduleCard(todaySlots)

        // NEW: feed the trend chart, only meaningful if the section is visible
        if (UserRoleUtil.canViewAnalytics(user.roleID)) {
            bindTrendChart(slots)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTodayScheduleCard(todaySlots: List<VwSlot>) {
        todaySlotsCache = todaySlots

        binding.todayScheduleCount.text = when (todaySlots.size) {
            0 -> "No slots"
            1 -> "1 slot"
            else -> "${todaySlots.size} slots"
        }
    }

    fun groupBySchoolHospital(
        slots: List<VwSlot>
    ): List<HospitalScheduleUi> {

        return slots
            .groupBy { Pair(it.dateSlot, it.hospitalID) }
            .values
            .map { group ->
                val first = group.first()
                HospitalScheduleUi(
                    schoolName = first.schoolName,
                    CIName = first.ci_fullname,
                    hospitalName = first.hospitalName,
                    date = first.dateSlot,
                    slots = group
                )
            }
    }

    @SuppressLint("ClickableViewAccessibility", "UseKtx")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTodayScheduleDialog() {
        val dialogBinding = DialogTodayScheduleBinding.inflate(layoutInflater)

        val dialog = Dialog(requireContext(), R.style.BottomDialogStyle).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
            window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        val todayAdapter = ScheduleAdapter(
            emptyList(),
            requireContext(),
            lifecycleOwner = viewLifecycleOwner
        ) { slot ->
            // same shift-click behavior as the main list, or leave as TODO
        }

        dialogBinding.todayScheduleList.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.todayScheduleList.adapter = todayAdapter

        dialogBinding.txtDate.text =  LocalDate.now().format(dateFormatter)

        // Wire the drag-to-dismiss + close button first so the dialog is
        // interactive immediately, even while data is still loading
        var startY = 0f

        dialogBinding.sheetContainer.post {
            // Capture the sheet's natural wrap_content height once it's laid out,
            // so we know what "collapsed" means when animating back down later
            collapsedSheetHeight = dialogBinding.sheetContainer.height
        }

        dialogBinding.sheetContainer.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = event.rawY - startY
                    // Only follow the finger for downward drags (dismiss/collapse gesture).
                    // Upward drags are handled as a discrete expand action on release,
                    // not a live-follow, since expanding requires a height change, not just translation.
                    if (delta > 0) {
                        v.translationY = delta
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val delta = event.rawY - startY
                    when {
                        // Dragged up past threshold -> expand to full screen
                        !isSheetExpanded && delta < -80 -> {
                            expandSheet(dialogBinding, v)
                        }
                        // Dragged down far enough -> collapse (if expanded) or dismiss (if already collapsed)
                        delta > v.height / 4 -> {
                            if (isSheetExpanded) {
                                collapseSheet(dialogBinding, v)
                            } else {
                                dialog.dismiss()
                            }
                        }
                        // Not far enough either direction -> snap back to current state
                        else -> {
                            v.animate().translationY(0f).setDuration(200).start()
                        }
                    }
                    true
                }
                else -> false
            }
        }


        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // Show spinner, hide list + empty state while fetching fresh data
        showDialogLoading(dialogBinding)

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.api(requireContext())
                val year = LocalDate.now().year

                val response = when (user.roleID) {
                    "UGR0001", "UGR0002" -> api.getSlots(year)
                    "UGR0003" -> api.getSlotsByUserID(user.userID, year)
                    "UGR0004" -> api.getSlotsByAppointUserID(user.userID, year)
                    "UGR0005" -> api.getSlotsByHospitalID(user.hospitalID ?: "", year)
                    "UGR0006" -> api.getSlotsByCI(user.userID, year)
                    else -> {
                        showDialogEmpty(dialogBinding)
                        return@launch
                    }
                }

                if (!dialog.isShowing) return@launch // user dismissed while loading

                if (response.isSuccessful && response.body() != null) {
                    val today = LocalDate.now()
                    val freshTodaySlots = response.body()!!.filter { slot ->
                        slot.slotStatus == 1 && try {
                            LocalDate.parse(slot.dateSlot) == today
                        } catch (e: Exception) {
                            false
                        }
                    }

                    todaySlotsCache = freshTodaySlots // keep cache in sync for next open
                    updateTodayScheduleCard(freshTodaySlots) // keep banner count in sync too

                    if (freshTodaySlots.isEmpty()) {
                        showDialogEmpty(dialogBinding)
                    } else {
                        val grouped = groupBySchoolHospital(freshTodaySlots)
                        todayAdapter.updateData(grouped)
                        showDialogList(dialogBinding)
                    }
                } else {
                    Log.e("HomeFragment_INFO", "Failed to fetch today's schedule")
                    showDialogEmpty(dialogBinding)
                }

            } catch (e: Exception) {
                Log.e("HomeFragment_INFO", "Error loading today's schedule dialog", e)
                if (dialog.isShowing) showDialogEmpty(dialogBinding)
            }
        }
    }

    private fun expandSheet(binding: DialogTodayScheduleBinding, sheet: View) {
        isSheetExpanded = true

        val displayHeight = resources.displayMetrics.heightPixels
        val startHeight = if (sheet.height > 0) sheet.height else collapsedSheetHeight

        ValueAnimator.ofInt(startHeight, displayHeight).apply {
            duration = 250
            addUpdateListener { anim ->
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = anim.animatedValue as Int
                }
            }
            start()
        }

        sheet.animate().translationY(0f).setDuration(250).start()

        // Let the list fill the newly available space
        binding.todayScheduleList.layoutParams = binding.todayScheduleList.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun collapseSheet(binding: DialogTodayScheduleBinding, sheet: View) {
        isSheetExpanded = false

        val startHeight = sheet.height

        ValueAnimator.ofInt(startHeight, collapsedSheetHeight).apply {
            duration = 250
            addUpdateListener { anim ->
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = anim.animatedValue as Int
                }
            }
            start()
        }

        sheet.animate().translationY(0f).setDuration(250).start()

        binding.todayScheduleList.layoutParams = binding.todayScheduleList.layoutParams.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }


    private fun bindStatusBreakdownChart(data: DashboardSummary) {
        val entries = listOf(
            BarEntry(0f, data.pendingSchedule.toFloat()),
            BarEntry(1f, data.confirmedSchedule.toFloat()),
            BarEntry(2f, data.totalAttendances.toFloat())
        )

        val dataSet = BarDataSet(entries, "Status").apply {
            colors = listOf(
                resources.getColor(R.color.amber, null),
                resources.getColor(R.color.accent_blue, null),
                resources.getColor(R.color.primary, null)
            )
            valueTextSize = 12f
        }

        binding.statusBarChart.apply {
            this.data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                    listOf("Pending", "Confirmed", "Attendance")
                )
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateY(600)
            invalidate()
        }
    }

    // Call this from processSlots(), after `slots` (the full-year, role-scoped, active list) is populated
    private fun bindTrendChart(allSlots: List<VwSlot>) {
        val monthCounts = IntArray(12)

        allSlots.forEach { slot ->
            try {
                val date = LocalDate.parse(slot.dateSlot)
                monthCounts[date.monthValue - 1]++
            } catch (e: Exception) {
                // skip unparsable dates
            }
        }

        val entries = monthCounts.mapIndexed { index, count ->
            Entry(index.toFloat(), count.toFloat())
        }

        val dataSet = LineDataSet(entries, "Slots").apply {
            color = resources.getColor(R.color.primary, null)
            setCircleColor(resources.getColor(R.color.primary, null))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.trendLineChart.apply {
            this.data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                    listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                )
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateX(600)
            invalidate()
        }
    }

    private fun showDialogLoading(binding: DialogTodayScheduleBinding) {
        binding.progressLoading.visibility = View.VISIBLE
        binding.todayScheduleList.visibility = View.GONE
        binding.dialogEmptyState.visibility = View.GONE
    }

    private fun showDialogList(binding: DialogTodayScheduleBinding) {
        binding.progressLoading.visibility = View.GONE
        binding.todayScheduleList.visibility = View.VISIBLE
        binding.dialogEmptyState.visibility = View.GONE
    }

    private fun showDialogEmpty(binding: DialogTodayScheduleBinding) {
        binding.progressLoading.visibility = View.GONE
        binding.todayScheduleList.visibility = View.GONE
        binding.dialogEmptyState.visibility = View.VISIBLE
    }

    private fun updateEmptyState(data: List<VwSlot>) {
        Log.d("HomeFragment_INFO", "data.isEmpty(): ${data.isEmpty()}")
        if (data.isEmpty()) {
            showEmptyState()
        } else {
            binding.list.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.noInternetState.visibility = View.GONE
        }
    }

    private fun showLoading() {
        binding.loadingState.visibility = View.VISIBLE
        binding.list.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.noInternetState.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingState.visibility = View.GONE
    }


    private fun showEmptyState() {
        binding.list.visibility = View.GONE
        binding.noInternetState.visibility = View.GONE

        binding.emptyState.apply {
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNoInternetState() {
        binding.list.visibility = View.GONE
        binding.emptyState.visibility = View.GONE

        binding.noInternetState.apply {
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
        }

        binding.btnRetry.setOnClickListener {
            loadRecentSchedule(user)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}