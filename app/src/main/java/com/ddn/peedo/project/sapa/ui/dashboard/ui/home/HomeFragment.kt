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
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.repository.OfflineSyncManager
import com.ddn.peedo.project.sapa.data.repository.toModel
import com.ddn.peedo.project.sapa.model.DashboardSummary
import com.ddn.peedo.project.sapa.model.VwSlot
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.util.UserRoleUtil
import com.google.gson.Gson
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private lateinit var user: VwUser

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

        // Wire retry before anything async runs
        binding.btnRetry.setOnClickListener {
            if (::user.isInitialized) loadDashboard(user) else initComponent()
        }

        initSyncProgressWatcher()
        initComponent()
        initRecycler()
    }

    // =========================================================
    // BACKGROUND REPLICATION PROGRESS — same blue banner as the
    // Schedules tab, shown on Home so the user sees the offline
    // copy being built before ever entering Schedules.
    // =========================================================

    private fun initSyncProgressWatcher() {
        viewLifecycleOwner.lifecycleScope.launch {
            OfflineSyncManager.progress.collect { p ->
                when {
                    p.active -> {
                        binding.syncProgressBanner.visibility = View.VISIBLE
                        binding.syncSpinner.visibility = View.VISIBLE
                        binding.syncProgressBar.visibility = View.VISIBLE
                        binding.syncProgressText.text =
                            "${p.phaseLabel} ${p.percent}%"
                        binding.syncProgressCounts.text =
                            if (p.phase == OfflineSyncManager.Progress.Phase.STUDENTS)
                                "${p.studentsDone} / ${p.studentsTotal} schedules"
                            else ""
                        binding.syncProgressBar.progress = p.percent
                    }

                    p.phase == OfflineSyncManager.Progress.Phase.DONE -> {
                        binding.syncProgressBanner.visibility = View.VISIBLE
                        binding.syncSpinner.visibility = View.GONE
                        binding.syncProgressBar.visibility = View.GONE
                        binding.syncProgressText.text =
                            "${p.phaseLabel} — 100%"
                        binding.syncProgressCounts.text = ""

                        view?.postDelayed({
                            if (_binding != null &&
                                OfflineSyncManager.progress.value.phase ==
                                OfflineSyncManager.Progress.Phase.DONE
                            ) {
                                binding.syncProgressBanner.visibility = View.GONE
                            }
                        }, 2500)
                    }

                    else -> binding.syncProgressBanner.visibility = View.GONE
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initComponent() {
        lifecycleScope.launch {

            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy",  Locale.ENGLISH)

            try {
                showLoading()
                val userJson = session.getUser()
                val privs = session.getPrivileges()


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

            } catch (e: Exception) {
                hideLoading()
                showNoInternetState()
                Log.e("HomeFragment_INFO", "Error: ${e.message}")
                return@launch
            }

            with(binding) {
                dateText.text = today.format(formatter)
                userDisplayName.text = "Hi ${user.firstname}, Good day!"

                swipeRefresh.setOnRefreshListener {
                    // Explicit refresh → bypass the freshness window
                    OfflineSyncManager.startAutoSync(requireContext(), force = true)
                    loadRecentSchedule(user)
                }

                todayScheduleCard.setOnClickListener {
                    showTodayScheduleDialog()
                }

                binding.btnRetry.setOnClickListener {
                    if (::user.isInitialized) {
                        loadDashboard(user)
                    }else {
                        initComponent()      // session load itself failed, restart everything
                    }
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

            binding.list.layoutManager = LinearLayoutManager(requireContext())
            binding.list.adapter = scheduleadapter
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = scheduleadapter



        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = scheduleadapter
//        binding.list.isNestedScrollingEnabled = false   // <-- add this line

        binding.list.post {
            Log.d(
                "HomeFragment_INFO",
                "RecyclerView height: ${binding.list.height}, item count: ${scheduleadapter.itemCount}"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadDashboard(user: VwUser) {

        lifecycleScope.launch {
            try {
                showLoading() // ← add this

                Log.d("HomeFragment_INFO", "Loading Dashboard: ${user.userID} - ${user.roleID}")
                val response = RetrofitClient.api(requireContext())
                    .getDashboardSummary(user.userID, user.roleID)

                if (response.isSuccessful && response.body() != null) {

                    val data = response.body()!!

                    Log.d("HomeFragment_INFO", "Loaded: ${Gson().toJson(data)}")

                    bindDashboard(data, user)

                } else {
                    hideLoading()

                    // Server rejected (offline mode / 401 etc.) — fall back to
                    // the Room replica so the dashboard still shows something.
                    showCachedDashboard(user)
                }

            } catch (e: Exception) {
                hideLoading()
                showCachedDashboard(user)
                Log.e("HomeFragment_INFO", "Error: ${e.message}")
            }
        }
    }

    /**
     * Offline fallback for the dashboard cards: computes the same summary
     * numbers from the Room replica (year-capped, role-scoped).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun showCachedDashboard(user: VwUser) {
        val db = SapaDatabase.getInstance(requireContext())

        val (fromDate, toDate) = OfflineSyncManager.cachedDateWindow()

        val slots = withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.slotDao().getAllOnce()
        }

        val scoped = slots.filter { s ->
            val inRole = when (user.roleID) {
                "UGR0003" -> s.userID == user.userID
                "UGR0006" -> s.CIID == user.userID
                "UGR0005" -> s.hospitalID == user.hospitalID
                else -> true // admins see everything
            }
            inRole && !s.dateSlot.isNullOrBlank() &&
                    s.dateSlot >= fromDate && s.dateSlot <= toDate
        }

        val summary = DashboardSummary(
            totalSlots = scoped.size,
            pendingSchedule = scoped.count { it.slotStatus == 0 },
            confirmedSchedule = scoped.count { it.slotStatus == 1 },
            declinedSchedule = scoped.count { it.slotStatus == 2 },
            cancellationRequest = scoped.count { it.slotStatus == 3 },
            cancelledSchedule = scoped.count { it.slotStatus == 4 },
            totalAppointedStudents = withContext(kotlinx.coroutines.Dispatchers.IO) {
                db.appointedStudentDao().getAllOnce().size
            },
            totalAttendances = withContext(kotlinx.coroutines.Dispatchers.IO) {
                db.attendanceDao().getAllOnce().size
            }
        )

        if (scoped.isEmpty()) {
            // Nothing cached at all — show the no-internet state
            showNoInternetState()
            return
        }

        bindDashboard(summary, user)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
                "UGR0003", "UGR0005", "UGR0006" -> {
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

        // replays correctly if the user goes offline again after retrying
        binding.noInternetState.alpha = 0f
        binding.noInternetState.scaleX = 0.9f
        binding.noInternetState.scaleY = 0.9f
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

                binding.swipeRefresh.isRefreshing = false
                list.clear()

                if (response.isSuccessful && response.body() != null) {
                    hideLoading()
                    processSlots(response.body()!!)
                } else {
                    hideLoading()
                    showCachedSlots(user)
                    Log.e("HomeFragment_INFO", "Failed to fetch slots — showing cached")
                }

            } catch (e: Exception) {
                hideLoading()
                showCachedSlots(user)
                Log.e("HomeFragment_INFO", "Error: ${e.message}")
            }
        }
    }

    /**
     * Offline fallback for the recent-schedules list: reads the Room replica.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun showCachedSlots(user: VwUser) {
        val db = SapaDatabase.getInstance(requireContext())

        val cached = withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.slotDao().getAllOnce().map { it.toModel() }
        }

        if (cached.isEmpty()) {
            showNoInternetState()
            return
        }

        // Same role filter as the API path
        val scoped = when (user.roleID) {
            "UGR0003" -> cached.filter { it.userID == user.userID }
            "UGR0006" -> cached.filter { it.CIID == user.userID }
            "UGR0005" -> cached.filter { it.hospitalID == user.hospitalID }
            else -> cached
        }

        processSlots(scoped)
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
        val today = LocalDate.now()

        recentSchedules = slots
            .filter { slot ->
                try {
                    LocalDate.parse(slot.dateSlot) <= today
                } catch (e: Exception) {
                    false
                }
            }
            .sortedByDescending { slot ->
                try {
                    LocalDate.parse(slot.dateSlot)
                } catch (e: Exception) {
                    LocalDate.MIN
                }
            }
            .take(10)

        val grouped = groupBySchoolHospital(recentSchedules)
        scheduleadapter.updateData(grouped)
        updateEmptyState(recentSchedules)

        // ✅ Today's slots computed separately, purely for the banner + dialog
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

        dialogBinding.txtDate.text = LocalDate.now().format(dateFormatter)

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

                var handled = false

                try {
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
                        handled = true
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment_INFO", "Today's schedule fetch failed — trying cache", e)
                }

                // OFFLINE FALLBACK — today's slots from the Room replica
                if (!handled && dialog.isShowing) {
                    val db = SapaDatabase.getInstance(requireContext())
                    val today = LocalDate.now()

                    val cachedToday = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        db.slotDao().getAllOnce().map { it.toModel() }
                    }.filter { slot ->
                        val inRole = when (user.roleID) {
                            "UGR0003" -> slot.userID == user.userID
                            "UGR0006" -> slot.CIID == user.userID
                            "UGR0005" -> slot.hospitalID == user.hospitalID
                            else -> true
                        }
                        inRole && slot.slotStatus == 1 &&
                                try {
                                    LocalDate.parse(slot.dateSlot) == today
                                } catch (e: Exception) {
                                    false
                                }
                    }

                    todaySlotsCache = cachedToday
                    updateTodayScheduleCard(cachedToday)

                    if (cachedToday.isEmpty()) {
                        showDialogEmpty(dialogBinding)
                    } else {
                        val grouped = groupBySchoolHospital(cachedToday)
                        todayAdapter.updateData(grouped)
                        showDialogList(dialogBinding)
                    }
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
                    listOf(
                        "Jan",
                        "Feb",
                        "Mar",
                        "Apr",
                        "May",
                        "Jun",
                        "Jul",
                        "Aug",
                        "Sep",
                        "Oct",
                        "Nov",
                        "Dec"
                    )
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
        binding.loadingState.visibility = View.VISIBLE
        binding.todayScheduleList.visibility = View.GONE
        binding.dialogEmptyState.visibility = View.GONE
    }

    private fun showDialogList(binding: DialogTodayScheduleBinding) {
        binding.loadingState.visibility = View.GONE
        binding.todayScheduleList.visibility = View.VISIBLE
        binding.dialogEmptyState.visibility = View.GONE
    }

    private fun showDialogEmpty(binding: DialogTodayScheduleBinding) {
        binding.loadingState.visibility = View.GONE
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
        }
    }

    private fun showLoading() {
        binding.loadingState.visibility = View.VISIBLE
        binding.content.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.noInternetState.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingState.visibility = View.GONE
        binding.content.visibility = View.VISIBLE
    }


    private fun showEmptyState() {
        binding.list.visibility = View.GONE
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

    private fun showNoInternetState() {
        binding.content.visibility = View.GONE
        binding.loadingState.visibility = View.GONE

        binding.noInternetState.apply {
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}