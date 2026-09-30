package com.ddn.peedo.project.sapa.ui.dashboard.ui.schedule

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ddn.peedo.project.sapa.adapter.CalendarAdapter
import com.ddn.peedo.project.sapa.adapter.ScheduleAdapter
import com.ddn.peedo.project.sapa.databinding.FragmentScheduleBinding
import com.ddn.peedo.project.sapa.dataclass.CalendarDay
import com.ddn.peedo.project.sapa.model.VwSlot
import java.time.LocalDate
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.dataclass.HospitalScheduleUi
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.store.SessionManager
import com.google.gson.Gson
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.view.View
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.repository.AttendanceQueueRepository
import com.ddn.peedo.project.sapa.data.repository.OfflineSyncManager
import com.ddn.peedo.project.sapa.work.AttendanceUploadWorker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val status = arrayOf(
        "ALL", "PENDING", "CONFIRM", "DECLINE", "CANCEL REQUEST", "CANCELED"
    )

    //    private lateinit var session: SessionManager;
    private val session by lazy {
        SessionManager(requireContext())
    }
    private var selectedItem = "ALL"

    @RequiresApi(Build.VERSION_CODES.O)
    private var currentMonth = java.time.YearMonth.now()

    @RequiresApi(Build.VERSION_CODES.O)
    private var selectedDate: LocalDate? = LocalDate.now()

    private val list = ArrayList<VwSlot>()

    private  lateinit var user: VwUser
    private lateinit var adapter: ScheduleAdapter
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var gestureDetector: GestureDetector
    private var isCalendarExpanded = true


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val schoolViewModel =
            ViewModelProvider(this)[ScheduleViewModel::class.java]

        Log.d(
            "ScheduleFragment_INFO",
            "Binding: "
        )
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        initRecycler()
        initSpinner()
        initCalendar()

        initPendingUploadWatcher()

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

            loadSchedule(user)

            with(binding) {

                binding.swipeRefresh.setOnRefreshListener {
                    resetFilters()
                    loadSchedule(user, force = true)
                }

            }




        }



    }


    // =========================================================
    // OFFLINE ATTENDANCE AUTO-UPLOAD
    //
    // Uploads start by themselves (post-scan flush + WorkManager),
    // so there is no upload button here. The amber banner appears
    // while records are waiting and flips to "Uploading N…" with a
    // live spinner while AttendanceQueueRepository drains the queue.
    // =========================================================

    private var isPendingUploadWatcherActive = false

    private fun initPendingUploadWatcher() {
        if (isPendingUploadWatcherActive) return
        isPendingUploadWatcherActive = true

        val queueDao = SapaDatabase.getInstance(requireContext()).attendanceQueueDao()
        val queueRepo = AttendanceQueueRepository(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            // Waiting count (queue table) + in-flight flag (repository) —
            // together they drive the automatic-upload banner.
            combine(
                queueDao.observePendingCount(),
                queueRepo.uploadState
            ) { pending, upload -> pending to upload }
                .collect { (pending, upload) ->
                    if (pending > 0) {
                        binding.pendingUploadBanner.visibility = View.VISIBLE

                        if (upload.uploading) {
                            // Upload in flight — show live progress; the
                            // waiting count drops as rows are marked synced.
                            binding.uploadSpinner.visibility = View.VISIBLE
                            binding.pendingUploadText.text =
                                "Uploading $pending attendance record(s)…"
                        } else {
                            binding.uploadSpinner.visibility = View.GONE
                            binding.pendingUploadText.text =
                                "$pending attendance record(s) waiting to upload"

                            // Opportunistic flush on resume/reconnect while visible
                            AttendanceUploadWorker.enqueueOneTimeFlush(requireContext())
                        }
                    } else {
                        binding.pendingUploadBanner.visibility = View.GONE
                    }
                }
        }
    }

    private fun initSpinner() {
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            status
        )

        binding.spinnerStatus.adapter = statusAdapter
        binding.spinnerStatus.setSelection(0, false)

        binding.spinnerStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedItem = parent.getItemAtPosition(position).toString()
                    applyCombinedFilter()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun initRecycler() {
        adapter = ScheduleAdapter(
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
        binding.list.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initCalendar() {

        calendarAdapter = CalendarAdapter { date ->
            selectedDate = date
            applyCombinedFilter()
            updateCalendar(list)
        }

        binding.calendarRecycler.layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(requireContext(), 7)
        binding.calendarRecycler.adapter = calendarAdapter

        binding.btnPrevMonth.setOnClickListener {
            selectedDate = null
            currentMonth = currentMonth.minusMonths(1)
            updateMonthTitle()
            updateCalendar(list)
        }

        binding.btnNextMonth.setOnClickListener {
            selectedDate = null
            currentMonth = currentMonth.plusMonths(1)
            updateMonthTitle()
            updateCalendar(list)
        }

        binding.btnToggleCalendar.setOnClickListener {
            toggleCalendar()
        }

        setupSwipeGesture()
        updateMonthTitle()
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeGesture() {

        gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val diffX = e2.x - e1.x
                    if (kotlin.math.abs(diffX) > 120 && kotlin.math.abs(velocityX) > 300) {
                        selectedDate = null
                        currentMonth =
                            if (diffX > 0) currentMonth.minusMonths(1)
                            else currentMonth.plusMonths(1)

                        updateMonthTitle()
                        updateCalendar(list)
                        return true
                    }
                    return false
                }
            }
        )

        binding.calendarRecycler.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        binding.calendarRecycler.addOnItemTouchListener(
            object : RecyclerView.OnItemTouchListener {

                override fun onInterceptTouchEvent(
                    rv: RecyclerView,
                    e: MotionEvent
                ): Boolean {

                    if (e.action == MotionEvent.ACTION_MOVE) {
                        rv.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    return false
                }

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            }
        )

    }

    private fun toggleCalendar() {
        val calendar = binding.calendarContainer
        if (isCalendarExpanded) {
            calendar.animate()
                .alpha(0f)
                .translationY(-calendar.height.toFloat() / 2)
                .setDuration(250)
                .withEndAction {
                    calendar.visibility = View.GONE
                }
                .start()
        } else {
            calendar.apply {
                alpha = 0f
                translationY = -height.toFloat() / 2
                visibility = View.VISIBLE

                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .start()
            }
        }
        isCalendarExpanded = !isCalendarExpanded
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateMonthTitle() {
        binding.txtMonthYear.text =
            buildString {
                append(
                    currentMonth.month.name.lowercase()
                        .replaceFirstChar { it.uppercase() })
                append(" ${currentMonth.year}")
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateCalendar(slots: List<VwSlot>) {

        val today = LocalDate.now()

        val grouped = slots
            .filter { it.dateSlot != null && it.slotStatus == 1 }
            .groupBy { LocalDate.parse(it.dateSlot) }

        val days = mutableListOf<CalendarDay>()

        val firstDay = currentMonth.atDay(1)
        val offset = firstDay.dayOfWeek.value - 1

        val prevMonth = currentMonth.minusMonths(1)
        val prevMonthDays = prevMonth.lengthOfMonth()

        for (i in offset downTo 1) {
            val date = prevMonth.atDay(prevMonthDays - i + 1)
            days.add(
                CalendarDay(
                    date,
                    false,
                    emptyList(),
                    isToday = date == today,
                    isSelected = date == selectedDate
                )
            )
        }

        for (day in 1..currentMonth.lengthOfMonth()) {
            val date = currentMonth.atDay(day)
            val schools = grouped[date]?.mapNotNull { it.schoolID }?.distinct() ?: emptyList()

            days.add(
                CalendarDay(
                    date,
                    true,
                    schools,
                    isToday = date == today,
                    isSelected = date == selectedDate
                )
            )
        }

        calendarAdapter.submit(days)
    }

    private fun updateLegend(slots: List<VwSlot>) {

        binding.legendContainer.removeAllViews()

        if (slots.isEmpty()) {
            binding.legendContainer.visibility = View.GONE
            return
        }

        val schools = slots
            .mapNotNull { slot ->
                if (slot.schoolID != null) {
                    slot.schoolID to slot.schoolName
                } else null
            }
            .distinctBy { it.first }

        if (schools.isEmpty()) {
            binding.legendContainer.visibility = View.GONE
            return
        }

        binding.legendContainer.visibility = View.VISIBLE

        schools.forEach { (id, name) ->
            val view = layoutInflater.inflate(
                R.layout.item_legend,
                binding.legendContainer,
                false
            )

            val dot = view.findViewById<View>(R.id.colorDot)
            val label = view.findViewById<TextView>(R.id.txtLabel)

            dot.background.setTint(
                com.ddn.peedo.project.sapa.utils.SchoolColorProvider
                    .getColor(requireContext(), id)
            )

            label.text = name ?: "School"

            binding.legendContainer.addView(view)
        }
    }

    private fun groupBySchoolHospital(
        slots: List<VwSlot>
    ): List<HospitalScheduleUi> {

        return slots
            .groupBy { slot ->
                Pair(slot.schoolID, slot.hospitalID)
            }
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


    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyCombinedFilter() {

        val filtered = list.filter { slot ->

            val matchesStatus =
                selectedItem == "ALL" ||
                        mapStatus(slot.slotStatus) == selectedItem

            val matchesDate =
                selectedDate == null ||
                        slot.dateSlot == selectedDate.toString()

            matchesStatus && matchesDate
        }
        Log.d("ScheduleFragment_INFO", "Slots: $filtered")

//        adapter.updateData(filtered)
        val grouped = groupBySchoolHospital(filtered)

        Log.d("ScheduleFragment_INFO", "Group Slots: $grouped")

        Log.d("ScheduleFragment_INFO", "Hospitals: ${grouped.size}")
        grouped.forEach {
            Log.d(
                "ScheduleFragment_INFO",
                "Hospital slots: ${it.slots.first().hospitalName} → ${it.slots.size}"
            )
        }
        adapter.updateData(grouped)
        updateLegend(filtered)
        updateEmptyState(filtered)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSchedule(user: VwUser, force: Boolean = false) {

        showLoading()

        lifecycleScope.launch {

            try {

                // SINGLE download path — schedules, appointed students and
                // attendance are ALL fetched & replicated into Room by the
                // OfflineSyncManager, with live % in the blue banner. When
                // offline it falls back to the Room cache automatically.
                // force = true only for explicit refreshes (swipe, Sync Now,
                // Reconnect): tab entry reuses a fresh replica instead of
                // re-downloading everything.
                val result =
                    OfflineSyncManager.runSync(requireContext(), force)

                hideLoading()

                binding.swipeRefresh.isRefreshing = false

                list.clear()

                when {

                    // =====================================================
                    // ONLINE — fresh data replicated into Room by the
                    // manager (schedules + interns + attendance, live %
                    // shown in the blue sync banner).
                    // =====================================================

                    result.fromServer -> {

                        binding.offlineBanner.visibility =
                            View.GONE

                        list.addAll(
                            result.slots
                        )

                        updateCalendar(
                            list
                        )

                        applyCombinedFilter()
                    }

                    // =====================================================
                    // OFFLINE — showing the Room cache
                    // =====================================================

                    !result.fromServer && result.slots.isNotEmpty() -> {

                        binding.offlineBanner.visibility =
                            View.VISIBLE

                        binding.btnReconnect.setOnClickListener {
                            loadSchedule(user, force = true)
                        }

                        val syncedText =
                            result.lastSyncedAt?.let {

                                val sdf =
                                    SimpleDateFormat(
                                        "MMM dd, h:mm a",
                                        Locale.ENGLISH
                                    )

                                "Offline — showing data synced ${
                                    sdf.format(
                                        Date(it)
                                    )
                                }"

                            } ?: "Offline — showing cached data"

                        binding.offlineBannerText.text =
                            syncedText

                        binding.btnSyncNow.setOnClickListener {
                            loadSchedule(user, force = true)
                        }

                        list.addAll(
                            result.slots
                        )

                        // ---------------------------------------------
                        // DO NOT call API here.
                        //
                        // AttendanceEntity already contains
                        // the last synchronized attendance.
                        // ---------------------------------------------

                        updateCalendar(
                            list
                        )

                        applyCombinedFilter()
                    }

                    // =====================================================
                    // NOTHING CACHED (offline & never synced)
                    // =====================================================

                    else -> {

                        binding.offlineBanner.visibility =
                            View.GONE

                        showNoInternetState()
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "ScheduleFragment",
                    "Error loading schedule",
                    e
                )

                hideLoading()

                binding.swipeRefresh.isRefreshing =
                    false
            }
        }
    }

    // (Attendance download moved into OfflineSyncManager — it runs as the
    // final phase of every replication pass, with live % in the banner.)

    private fun mapStatus(status: Int?): String {
        return when (status) {
            0 -> "PENDING"
            1 -> "CONFIRM"
            2 -> "DECLINE"
            3 -> "CANCEL REQUEST"
            4 -> "CANCELED"
            else -> "UNKNOWN"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun resetFilters() {
        selectedDate = LocalDate.now()
        selectedItem = "ALL"
        binding.spinnerStatus.setSelection(0, false)
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

    private fun updateEmptyState(data: List<VwSlot>) {
        Log.d("ScheduleFragment_INFO", "data.isEmpty(): ${data.isEmpty()}")
        if (data.isEmpty()) {
            showEmptyState()
        } else {
            binding.list.visibility = View.VISIBLE
            binding.legendContainer.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.noInternetState.visibility = View.GONE
        }
    }


    private fun showEmptyState() {
        binding.list.visibility = View.GONE
        binding.noInternetState.visibility = View.GONE
        binding.legendContainer.visibility = View.GONE

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
            loadSchedule(user)
        }

    }

}