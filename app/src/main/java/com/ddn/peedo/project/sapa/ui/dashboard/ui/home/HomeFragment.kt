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
import androidx.recyclerview.widget.LinearLayoutManager
import com.ddn.peedo.project.sapa.adapter.RecentScheduleAdapter
import com.ddn.peedo.project.sapa.adapter.ScheduleAdapter
import com.ddn.peedo.project.sapa.databinding.FragmentHomeBinding
import com.ddn.peedo.project.sapa.dataclass.HospitalScheduleUi
import com.ddn.peedo.project.sapa.model.DashboardSummary
import com.ddn.peedo.project.sapa.model.VwSlot
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.store.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
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

    private  lateinit var user: VwUser

    private var slots: List<VwSlot> = emptyList()
    private val list = ArrayList<VwSlot>()

    private var recentSchedules: List<VwSlot> = emptyList()
    private lateinit var scheduleadapter: ScheduleAdapter

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

        initRecycler()
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
            }

            Log.d("HomeFragment_INFO", "SESSION User: $user")

            loadDashboard(user)
            loadRecentSchedule(user)

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
        }
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

        if (data.isEmpty()) return

        Log.d("HomeFragment_INFO", "All Slots: ${Gson().toJson(data)}")

        // ✅ Filter active slots
        slots = data.filter { it.slotStatus == 1 }

        // ✅ Sort by date_created DESC
        recentSchedules = slots
            .sortedByDescending {
                try {
                    LocalDate.parse(it.dateSlot)
                } catch (e: Exception) {
                    LocalDate.MIN
                }
            }
            .take(10)

        Log.d("HomeFragment_INFO", "Recent: ${Gson().toJson(recentSchedules)}")

        val grouped = groupBySchoolHospital(recentSchedules)

        Log.d("HomeFragment_INFO", "Grouped size: ${grouped.size}")

        scheduleadapter.updateData(grouped)
        updateEmptyState(recentSchedules)
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