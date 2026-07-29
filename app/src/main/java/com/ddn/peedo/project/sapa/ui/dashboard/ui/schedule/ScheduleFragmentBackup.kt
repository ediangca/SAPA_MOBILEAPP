package com.ddn.peedo.project.sapa.ui.dashboard.ui.schedule

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
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
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.dataclass.HospitalScheduleUi
import com.ddn.peedo.project.sapa.utils.SchoolColorProvider
import java.time.YearMonth
import kotlin.math.abs


class ScheduleFragmentBackup : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private var status =
        arrayOf(
            "ALL",
            "PENDING",
            "CONFIRM",
            "DECLINE",
            "CANCEL REQUEST",
            "CANCELED",
        )

    private var selectedItem: String = "ALL"

    private var list: ArrayList<VwSlot> = ArrayList()
    private lateinit var adapter: ScheduleAdapter
    private lateinit var calendarAdapter: CalendarAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    private var currentMonth = YearMonth.now()

    @RequiresApi(Build.VERSION_CODES.O)
    private var selectedDate: LocalDate? = LocalDate.now()

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

        initComponent()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun initComponent() {

        selectedDate = LocalDate.now()

        var statusAdapter =
            context?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, status) }


        with(binding.spinnerStatus) {

            adapter = statusAdapter
            setSelection(0, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    this@ScheduleFragmentBackup.selectedItem =
                        parent.getItemAtPosition(position).toString() // Get the selected item

                    Log.d(
                        "ScheduleFragment_INFO",
                        "Selected Item: " + this@ScheduleFragmentBackup.selectedItem
                    )
//                    showSchedule(this@ScheduleFragment.selectedItem);
                    applyCombinedFilter()

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Optionally handle the case where no item is selected

                }
            }

            prompt = "Select Status"
            gravity = Gravity.CENTER

        }
        initRecycler()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initCalendar()
        }

        binding.swipeRefresh.setOnRefreshListener {
            selectedDate = LocalDate.now()
            selectedItem = "ALL"
            binding.spinnerStatus.setSelection(0, false)
            loadSchedule()
        }
        binding.btnToggleCalendar.setOnClickListener {
            toggleCalendar()
        }

        loadSchedule()

    }


    private fun initRecycler() {
//        adapter = ScheduleAdapter(emptyList())
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
    }


    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initCalendar() {

        calendarAdapter = CalendarAdapter { date ->
            selectedDate = date
            applyCombinedFilter()
            updateCalendar(list)
        }

        binding.calendarRecycler.layoutManager =
            GridLayoutManager(requireContext(), 7)

        binding.calendarRecycler.adapter = calendarAdapter

        binding.btnPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)

            binding.legendContainer.removeAllViews()
            updateMonthTitle()
            updateCalendar(list)
        }

        binding.btnNextMonth.setOnClickListener {
            selectedDate = null
            currentMonth = currentMonth.plusMonths(1)

            binding.legendContainer.removeAllViews()
            updateMonthTitle()
            updateCalendar(list)

        }

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
                    val absDiffX = abs(diffX)
                    val absVelocityX = abs(velocityX)

                    if (absDiffX > 120 && absVelocityX > 300) {
                        if (diffX > 0) {
                            currentMonth = currentMonth.minusMonths(1)
                        } else {
                            currentMonth = currentMonth.plusMonths(1)
                        }

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
            false   // 👈 MUST be false
        }

        updateMonthTitle()
    }

    private fun toggleCalendar() {
        val appBar = binding.appBarLayout
        if (isCalendarExpanded) {
            appBar.setExpanded(false, true)
        } else {
            appBar.setExpanded(true, true)
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
            .filter { it.dateSlot != null }
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

//    private fun updateLegend(slots: List<VwSlot>) {
//
//        binding.legendContainer.removeAllViews()
//
//        val schools = slots
//            .mapNotNull { it.schoolID to it.schoolName }
//            .distinctBy { it.first }
//
//        schools.forEach { (id, name) ->
//            val view = layoutInflater.inflate(R.layout.item_legend, binding.legendContainer, false)
//
//            val dot = view.findViewById<View>(R.id.colorDot)
//            val label = view.findViewById<TextView>(R.id.txtLabel)
//
//            dot.background.setTint(
//                com.ddn.peedo.project.sapa.utils.SchoolColorProvider
//                    .getColor(requireContext(), id)
//            )
//
//            label.text = name ?: "School"
//
//            binding.legendContainer.addView(view)
//        }
//    }

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
                SchoolColorProvider
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
//                selectedDate ||
                slot.dateSlot == selectedDate.toString()

            matchesStatus && matchesDate
        }

//        adapter.updateData(filtered)
        adapter.updateData(groupBySchoolHospital(filtered))
        updateEmptyState(filtered)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSchedule() {
        showLoading()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.create(requireContext()).getSchedule()

                hideLoading()
                binding.swipeRefresh.isRefreshing = false
                list = ArrayList()

                if (response.isSuccessful) {
                    list = ArrayList(response.body() ?: emptyList())

//                    updateCalendar(list)
                    applyCombinedFilter()
                    Log.d(
                        "ScheduleFragment_INFO",
                        "Success: " + response.body()
                    )
                    updateEmptyState(list)
                } else {
                    showEmptyState()
                    updateEmptyState(list)
                    Log.d(
                        "ScheduleFragment_INFO",
                        "Error: " + response.message()
                    )
                    hideLoading()
                    binding.swipeRefresh.isRefreshing = false
                    showNoInternetState()
                }


                updateLegend(list)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(
                    "ScheduleFragment_INFO",
                    "Error: " + e.printStackTrace()
                )
                hideLoading()
                binding.swipeRefresh.isRefreshing = false
                showNoInternetState()
            }
        }
    }

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
            loadSchedule()
        }
    }

}