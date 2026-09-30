package com.ddn.peedo.project.sapa.ui.dashboard.ui.users

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.util.Log
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.local.entity.UserEntity
import com.ddn.peedo.project.sapa.data.repository.OfflineSyncManager
import com.ddn.peedo.project.sapa.databinding.FragmentUserBinding
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.services.ApiService
import com.ddn.peedo.project.sapa.utils.UserStatusUtil
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.widget.addTextChangedListener
import com.ddn.peedo.project.sapa.store.SessionManager
import com.ddn.peedo.project.sapa.util.UserRoleUtil
import kotlinx.coroutines.delay

class UsersFragment : Fragment(), UserAdapter.UserActionListener {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: ApiService
    private lateinit var adapter: UserAdapter
    private val session by lazy {
        SessionManager(requireContext())
    }
    private var allUsers: List<VwUser> = emptyList()
    private var currentUserId: String? = null
    private var currentUserRoleId: String? = null
    private var currentUserSchoolId: String? = null
    private var currentUserCoorSchoolId: String? = null
    private val statusFilters =
        listOf("All", "Unverified", "Pending", "Approved", "Suspended", "Inactive")

    private var roleFilters: List<String> = listOf("All")

    private val excludedRoleIds = setOf(UserRoleUtil.SYSTEM_ADMIN, UserRoleUtil.ADMIN)

    private var currentSearchQuery: String = ""
    private var searchDebounceJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = RetrofitClient.api(requireContext())

        setupRecyclerView()
        setupStatusSpinner()
        setupSearch()
        setupFilterToggle()
        setupSwipeRefresh()

        binding.btnOfflineRetry.setOnClickListener { loadUsers() }

        loadUsers()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { editable ->
            searchDebounceJob?.cancel()
            searchDebounceJob = lifecycleScope.launch {
                delay(300)
                currentSearchQuery = editable?.toString().orEmpty().trim()
                applyFilters()
            }
        }
    }

    private fun setupFilterToggle() {
        binding.btnToggleFilter.setOnClickListener {
            binding.filterContainer.isVisible = !binding.filterContainer.isVisible
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(this)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
    }

    private fun setupStatusSpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            statusFilters
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerStatus.adapter = spinnerAdapter

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
//                applyFilter(statusFilters[position])
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRoleSpinner(roles: List<String>) {
        roleFilters = listOf("All") + roles

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            roleFilters
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Preserve current selection across refreshes when possible
        val previouslySelected = (binding.spinnerRole.selectedItem as? String) ?: "All"
        binding.spinnerRole.adapter = spinnerAdapter
        val restoredIndex = roleFilters.indexOf(previouslySelected).takeIf { it >= 0 } ?: 0
        binding.spinnerRole.setSelection(restoredIndex)

        binding.spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { loadUsers() }
    }

    private fun loadUsers() {
        showLoading()
        lifecycleScope.launch {
            try {
                val sessionUser = session.getUser()
                Log.d("UsersFragment", "Session user raw JSON: $sessionUser")

                currentUserId = sessionUser?.optString("userID")?.takeIf { it.isNotBlank() }
                currentUserRoleId = sessionUser?.optString("roleID")?.takeIf { it.isNotBlank() }
                currentUserSchoolId = sessionUser?.optString("schoolID")?.takeIf { it.isNotBlank() }
                currentUserCoorSchoolId =
                    sessionUser?.optString("coorSchoolID")?.takeIf { it.isNotBlank() }

                Log.d(
                    "UsersFragment", "currentUserId=$currentUserId, " +
                            "currentUserRoleId=$currentUserRoleId, currentUserSchoolId=$currentUserSchoolId, " +
                            "currentUserCoorSchoolId=$currentUserCoorSchoolId"
                )

                // Coordinators and CIs get a view-only directory — no
                // Approve / Resend Verification actions.
                adapter.setReadOnly(isViewOnlyDirectory())

                val response = api.getUsers()

                binding.swipeRefresh.isRefreshing = false

                if (currentUserRoleId == null) {
                    Log.e(
                        "UsersFragment",
                        "roleID missing from session — check SessionManager keys"
                    )
                }

                if (response.isSuccessful) {
                    val fetchedUsers = response.body().orEmpty()
                    Log.d("UsersFragment", "Fetched ${fetchedUsers.size} users from API")

                    allUsers = scopeUsersByRole(fetchedUsers)
                    Log.d(
                        "UsersFragment",
                        "After role/school scoping: ${allUsers.size} users remain"
                    )

                    // Build role filter options from actual data, excluding admin/default roles
                    val distinctRoles = allUsers
                        .filter { it.roleID !in excludedRoleIds }
                        .map { it.rolename }
                        .distinct()
                        .sorted()
                    Log.d("UsersFragment", "Role filter options built: $distinctRoles")
                    setupRoleSpinner(distinctRoles)

                    applyFilters()
                    hideLoading()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "UsersFragment",
                        "getUsers() failed: code=${response.code()}, body=$errorBody"
                    )
                    showOfflineDirectory("Couldn't reach the server — showing cached directory")
                }
            } catch (e: Exception) {
                Log.d("UsersFragment", "Error: " + e.message)
                binding.swipeRefresh.isRefreshing = false
                showOfflineDirectory("Offline — showing cached directory")
            }
        }
    }

    /**
     * Who sees what in the Users directory:
     *  - System Admin / SAP Admin -> every user, no restrictions.
     *  - School Coordinator (UGR0003) -> CIs + students belonging to their
     *    school, combined into one list.
     *  - Clinical Instructor (UGR0006) -> students belonging to their school.
     *  - Any other role -> no access (empty list).
     */
    private fun scopeUsersByRole(users: List<VwUser>): List<VwUser> {
        val roleId = currentUserRoleId

        if (roleId == null) {
            Log.e("UsersFragment", "scopeUsersByRole: roleId is null, returning empty list")
            return emptyList()
        }

        // ---------------- Admin tier: full directory ----------------
        if (roleId == UserRoleUtil.SYSTEM_ADMIN ||
            roleId == UserRoleUtil.ADMIN ||
            roleId == UserRoleUtil.SAP_ADMIN
        ) {
            Log.d(
                "UsersFragment",
                "scopeUsersByRole: admin-tier role ($roleId), showing all users"
            )
            return users.filter { it.userID != currentUserId }
        }

        // -------- Coordinator / CI: role + school scoped view --------
        val visibleRoles = UserRoleUtil.usersDirectoryVisibleRoles[roleId]
        if (visibleRoles == null) {
            Log.w(
                "UsersFragment",
                "scopeUsersByRole: role ($roleId) has no Users directory access, returning empty list"
            )
            return emptyList()
        }

        // School scope: the session schoolID. Coordinators may alternatively
        // be linked to their school through coorSchoolID, so both are
        // accepted when present.
        val scopedSchoolIds = buildList {
            currentUserSchoolId?.let { add(it) }
            if (roleId == UserRoleUtil.SCHOOL_COORDINATOR) {
                currentUserCoorSchoolId?.let { add(it) }
            }
        }.distinct()

        if (scopedSchoolIds.isEmpty()) {
            Log.e(
                "UsersFragment",
                "scopeUsersByRole: $roleId has no school scope in session, returning empty list"
            )
            return emptyList()
        }

        val filtered = users.filter { user ->
            user.roleID in visibleRoles && user.schoolID in scopedSchoolIds
        }
        Log.d(
            "UsersFragment",
            "scopeUsersByRole: $roleId, schools=$scopedSchoolIds, " +
                    "visibleRoles=$visibleRoles, matched ${filtered.size}/${users.size}"
        )

        return filtered.filter { it.userID != currentUserId }
    }


    private fun applyFilters() {
        val selectedStatus =
            statusFilters.getOrElse(binding.spinnerStatus.selectedItemPosition) { "All" }
        val selectedRole = roleFilters.getOrElse(binding.spinnerRole.selectedItemPosition) { "All" }

        var filtered = allUsers.filter { it.roleID !in excludedRoleIds }

        filtered = when (selectedStatus) {
            "Unverified" -> filtered.filter { it.status == UserStatusUtil.UNVERIFIED }
            "Pending" -> filtered.filter { it.status == UserStatusUtil.PENDING }
            "Approved" -> filtered.filter { it.status == UserStatusUtil.APPROVED }
            "Suspended" -> filtered.filter { it.status == UserStatusUtil.SUSPENDED }
            "Inactive" -> filtered.filter { it.status == UserStatusUtil.INACTIVE }
            else -> filtered
        }

        if (selectedRole != "All") {
            filtered = filtered.filter { it.rolename == selectedRole }
        }

        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.fullname.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState()
        } else {
            showList()
            adapter.submitList(filtered)
        }
    }

    // ---------- View-only directory (Coordinator / CI) ----------

    private fun isViewOnlyDirectory(): Boolean {
        val roleId = currentUserRoleId ?: return true
        return roleId == UserRoleUtil.SCHOOL_COORDINATOR ||
                roleId == UserRoleUtil.CLINICAL_INSTRUCTOR
    }

    // ---------- OFFLINE FALLBACK (Room users replica) ----------

    /**
     * Shows the directory from the offline replica replicated by
     * OfflineSyncManager, with the same offline-banner pattern as the
     * Schedules tab. Applied role/school scoping keeps every role's view
     * identical online and offline.
     */
    private fun showOfflineDirectory(message: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = SapaDatabase.getInstance(requireContext())

                val scoped = scopeUsersByRole(db.userDao().getAllOnce().map { it.toVwUser() })

                val lastSyncedAt = db.syncMetaDao().get(OfflineSyncManager.USERS_MODULE_NAME)
                    ?.lastSyncedAt
                        ?: db.syncMetaDao().get(OfflineSyncManager.SCHOOLS_MODULE_NAME)
                            ?.lastSyncedAt
                        ?: OfflineSyncManager.lastSyncTime(requireContext())

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext

                    binding.offlineBanner.visibility = View.VISIBLE
                    binding.offlineBannerText.text = if (lastSyncedAt > 0) {
                        "$message (synced ${sdfSynced.format(Date(lastSyncedAt))})"
                    } else {
                        message
                    }

                    if (scoped.isEmpty()) {
                        showNoInternetState()
                    } else {
                        allUsers = scoped
                        setupRoleSpinner(
                            scoped.filter { it.roleID !in excludedRoleIds }
                                .map { it.rolename }
                                .distinct()
                                .sorted()
                        )
                        applyFilters()
                    }

                    hideLoading()
                }
            } catch (e: Exception) {
                Log.e("UsersFragment", "Offline directory fallback failed", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        hideLoading()
                        showNoInternetState()
                    }
                }
            }
        }
    }

    /** Room → API model. Mirrors the mapper used by SchoolFragment. */
    private fun UserEntity.toVwUser() = VwUser(
        userID = userID,
        username = username,
        password = "",
        lastname = lastname,
        firstname = firstname,
        middlename = middlename,
        fullname = fullname,
        email = email,
        emailVerifiedAt = null,
        roleID = roleID,
        rolename = rolename,
        schoolID = schoolID,
        schoolName = schoolName,
        status = status,
        coorSchoolID = coorSchoolID,
        coorSchoolCode = coorSchoolCode,
        coorSchoolName = coorSchoolName,
        hospitalID = hospitalID,
        hospitalName = hospitalName,
        dateCreated = dateCreated,
        dateUpdated = dateUpdated
    )

    // ---------- USER DETAIL BOTTOM SHEET ----------

    private val sdfSynced = SimpleDateFormat("MMM dd, h:mm a", Locale.ENGLISH)
    private val sdfMemberSince = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    private fun showUserDetail(user: VwUser) {
        val sheet = layoutInflater.inflate(R.layout.sheet_user_detail, null)

        sheet.findViewById<TextView>(R.id.txtName).text = user.fullname
        sheet.findViewById<TextView>(R.id.txtRole).text =
            user.rolename.ifBlank { user.roleID }
        sheet.findViewById<TextView>(R.id.txtEmail).text = user.email
        sheet.findViewById<TextView>(R.id.txtSchool).text =
            user.schoolName ?: "No school assigned"

        val statusView = sheet.findViewById<TextView>(R.id.txtStatus)
        statusView.text = UserStatusUtil.label(user.status)
        statusView.background.setTint(statusColor(user.status))

        val hospitalRow = sheet.findViewById<View>(R.id.hospitalRow)
        if (user.hospitalName.isNullOrBlank()) {
            hospitalRow.visibility = View.GONE
        } else {
            hospitalRow.visibility = View.VISIBLE
            sheet.findViewById<TextView>(R.id.txtHospital).text = user.hospitalName
        }

        sheet.findViewById<TextView>(R.id.txtMemberSince).text =
            parseMemberSince(user.dateCreated)

        // Offline activity summary — a view-only reference, not an audit:
        // counts come from the cached replica and include LOCAL-* offline
        // scans, so a coordinator can see records still queued for upload.
        lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                val db = SapaDatabase.getInstance(requireContext())
                Triple(
                    db.appointedStudentDao().countByUser(user.userID),
                    db.attendanceDao().countByUser(user.userID),
                    db.slotDao().countByCI(user.userID)
                )
            }

            if (_binding == null) return@launch

            sheet.findViewById<TextView>(R.id.txtAppointments).text =
                counts.first.toString()
            sheet.findViewById<TextView>(R.id.txtAttendance).text =
                counts.second.toString()

            val ciText = sheet.findViewById<TextView>(R.id.txtCiSlots)
            if (counts.third > 0) {
                ciText.visibility = View.VISIBLE
                ciText.text = "CI for ${counts.third} schedule(s)"
            }
        }

        BottomSheetDialog(requireContext()).apply {
            setContentView(sheet)
            show()
        }
    }

    private fun parseMemberSince(dateCreated: String): String = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).parse(dateCreated)?.let {
            sdfMemberSince.format(it)
        } ?: dateCreated
    } catch (e: Exception) {
        dateCreated
    }

    private fun statusColor(status: Char?): Int = when (status) {
        UserStatusUtil.APPROVED -> Color.parseColor("#003366")   // primary
        UserStatusUtil.PENDING -> Color.parseColor("#F0A500")    // amber
        UserStatusUtil.UNVERIFIED -> Color.parseColor("#607D8B") // blue-grey
        UserStatusUtil.SUSPENDED -> Color.parseColor("#C62828")  // red
        UserStatusUtil.INACTIVE -> Color.parseColor("#9E9E9E")   // grey
        else -> Color.GRAY
    }

    // ---------- UserAdapter.UserActionListener ----------

    override fun onApprove(user: VwUser) {
        SweetAlertUtil.showConfirm(
            requireContext(),
            "Approve Account",
            "Are you sure you want to approve ${user.fullname}?",
            confirmText = "Yes, Approve",
            cancelText = "Cancel"
        ) {
            approveUser(user)
        }
    }

    override fun onResendVerification(user: VwUser) {
        SweetAlertUtil.showConfirm(
            requireContext(),
            "Resend Verification",
            "Are you sure you want to resend email verification to ${user.email}?",
            confirmText = "Yes, Resend",
            cancelText = "Cancel"
        ) {
            resendVerification(user)
        }
    }

    override fun onItemClick(user: VwUser) {
        showUserDetail(user)
    }

    private fun approveUser(user: VwUser) {
        adapter.setLoading(user.userID, true)
        lifecycleScope.launch {
            try {
                val response = api.approveUser(user.userID)
                adapter.setLoading(user.userID, false)
                if (response.isSuccessful) {
                    SweetAlertUtil.showSuccess(
                        requireContext(),
                        "Approved",
                        "${user.fullname} has been approved!"
                    )
                    loadUsers()
                } else {
                    SweetAlertUtil.showError(
                        requireContext(),
                        "Failed to approve",
                        "Error ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                adapter.setLoading(user.userID, false)
                SweetAlertUtil.showError(
                    requireContext(),
                    "Failed to approve",
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun resendVerification(user: VwUser) {
        adapter.setLoading(user.userID, true)
        lifecycleScope.launch {
            try {
                val response = api.resendVerification(user.email)
                adapter.setLoading(user.userID, false)
                if (response.isSuccessful) {
                    SweetAlertUtil.showSuccess(
                        requireContext(),
                        "Verification Sent",
                        "Verification successfully sent!"
                    )
                    loadUsers()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                    SweetAlertUtil.showError(
                        requireContext(),
                        "Failed to resend verification",
                        errorMsg
                    )
                }
            } catch (e: Exception) {
                adapter.setLoading(user.userID, false)
                SweetAlertUtil.showError(
                    requireContext(),
                    "Failed to resend verification",
                    e.message ?: "Unknown error"
                )
            }
        }
    }


    // ---------- State helpers ----------

    private fun showLoading() {
        binding.loadingState.visibility = View.VISIBLE
        binding.list.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.noInternetState.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingState.visibility = View.GONE
    }

    private fun showList() {
        binding.loadingState.visibility = View.GONE
        binding.list.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.noInternetState.visibility = View.GONE
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
            loadUsers()
        }
    }

    private fun showSuccessAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}