package com.ddn.peedo.project.sapa.ui.dashboard.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ddn.peedo.project.sapa.databinding.FragmentUserBinding
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.services.ApiService
import com.ddn.peedo.project.sapa.utils.UserStatusUtil
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.UnknownHostException
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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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

                Log.d("UsersFragment", "currentUserId=$currentUserId, " +
                        "currentUserRoleId=$currentUserRoleId, currentUserSchoolId=$currentUserSchoolId")

                val response = api.getUsers()
                binding.swipeRefresh.isRefreshing = false

                if (currentUserRoleId == null) {
                    Log.e("UsersFragment", "roleID missing from session — check SessionManager keys")
                }

                if (response.isSuccessful) {
                    val fetchedUsers = response.body().orEmpty()
                    Log.d("UsersFragment", "Fetched ${fetchedUsers.size} users from API")

                    allUsers = scopeUsersByRole(fetchedUsers)
                    Log.d("UsersFragment", "After role/school scoping: ${allUsers.size} users remain")

                    // Build role filter options from actual data, excluding admin/default roles
                    val distinctRoles = allUsers
                        .filter { it.roleID !in excludedRoleIds }
                        .map { it.rolename }
                        .distinct()
                        .sorted()
                    Log.d("UsersFragment", "Role filter options built: $distinctRoles")
                    setupRoleSpinner(distinctRoles)

                    applyFilters()
//                    applyFilter(statusFilters[binding.spinnerStatus.selectedItemPosition])
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UsersFragment", "getUsers() failed: code=${response.code()}, body=$errorBody")
                    showEmpty("Failed to load users (${response.code()})")
                    SweetAlertUtil.showError(
                        requireContext(),
                        "Failed to Load Users",
                        "Server returned an error (code ${response.code()})."
                    )
                }
            } catch (e: UnknownHostException) {
                Log.e("UsersFragment", "No internet connection", e)
                binding.swipeRefresh.isRefreshing = false
                showNoInternet()
            } catch (e: Exception) {
                Log.e("UsersFragment", "Unexpected error loading users", e)
                binding.swipeRefresh.isRefreshing = false
                showEmpty("Something went wrong: ${e.message}")
                SweetAlertUtil.showError(
                    requireContext(),
                    "Something Went Wrong",
                    e.message ?: "Unknown error occurred while loading users."
                )
            }
        }
    }

    /**
     * System Admin / Admin -> sees every user, no restrictions.
     * School Coordinator (and anyone else school-scoped) -> only sees users
     * belonging to their own school, and only within Coordinator/Instructor/Student roles.
     */
    private fun scopeUsersByRole(users: List<VwUser>): List<VwUser> {
        val roleId = currentUserRoleId

        val roleScoped = when {
            roleId == null -> {
                Log.e("UsersFragment", "scopeUsersByRole: roleId is null, returning empty list")
                emptyList()
            }
            roleId in UserRoleUtil.adminTierRoles -> {
                Log.d("UsersFragment", "scopeUsersByRole: admin-tier role ($roleId), showing all users")
                users
            }
            else -> {
                val schoolId = currentUserSchoolId
                if (schoolId.isNullOrBlank()) {
                    Log.e("UsersFragment", "scopeUsersByRole: non-admin role ($roleId) but schoolId is null/blank, returning empty list")
                    emptyList()
                } else {
                    val filtered = users.filter { user ->
                        user.roleID in UserRoleUtil.schoolScopedRoles && user.schoolID == schoolId
                    }
                    Log.d("UsersFragment", "scopeUsersByRole: school-scoped role ($roleId), schoolId=$schoolId, matched ${filtered.size}/${users.size}")
                    filtered
                }
            }
        }

        return roleScoped.filter { it.userID != currentUserId }
    }

    private fun applyFilter(filter: String) {
        val filtered = when (filter) {
            "Unverified" -> allUsers.filter { it.status == UserStatusUtil.UNVERIFIED }
            "Pending" -> allUsers.filter { it.status == UserStatusUtil.PENDING }
            "Approved" -> allUsers.filter { it.status == UserStatusUtil.APPROVED }
            "Suspended" -> allUsers.filter { it.status == UserStatusUtil.SUSPENDED }
            "Inactive" -> allUsers.filter { it.status == UserStatusUtil.INACTIVE }
            else -> allUsers
        }

        if (filtered.isEmpty()) {
            showEmpty("No users found")
        } else {
            showList()
            adapter.submitList(filtered)
        }
    }

    private fun applyFilters() {
        val selectedStatus = statusFilters.getOrElse(binding.spinnerStatus.selectedItemPosition) { "All" }
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
            showEmpty(
                if (currentSearchQuery.isNotEmpty()) "No users found matching \"$currentSearchQuery\""
                else "No users found"
            )
        } else {
            showList()
            adapter.submitList(filtered)
        }
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

    private fun approveUser(user: VwUser) {
        adapter.setLoading(user.userID, true)
        lifecycleScope.launch {
            try {
                val response = api.approveUser(user.userID)
                adapter.setLoading(user.userID, false)
                if (response.isSuccessful) {
                    SweetAlertUtil.showSuccess(requireContext(), "Approved", "${user.fullname} has been approved!")
                    loadUsers()
                } else {
                    SweetAlertUtil.showError(requireContext(), "Failed to approve", "Error ${response.code()}")
                }
            } catch (e: Exception) {
                adapter.setLoading(user.userID, false)
                SweetAlertUtil.showError(requireContext(), "Failed to approve", e.message ?: "Unknown error")
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
                    SweetAlertUtil.showSuccess(requireContext(), "Verification Sent", "Verification successfully sent!")
                    loadUsers()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                    SweetAlertUtil.showError(requireContext(), "Failed to resend verification", errorMsg)
                }
            } catch (e: Exception) {
                adapter.setLoading(user.userID, false)
                SweetAlertUtil.showError(requireContext(), "Failed to resend verification", e.message ?: "Unknown error")
            }
        }
    }

    // ---------- State helpers ----------

    private fun showLoading() {
        binding.loadingState.isVisible = true
        binding.list.isVisible = false
        binding.emptyState.isVisible = false
        binding.noInternetState.isVisible = false
    }

    private fun showList() {
        binding.loadingState.isVisible = false
        binding.list.isVisible = true
        binding.emptyState.isVisible = false
        binding.noInternetState.isVisible = false
    }

    private fun showEmpty(message: String) {
        binding.loadingState.isVisible = false
        binding.list.isVisible = false
        binding.emptyState.isVisible = true
        binding.noInternetState.isVisible = false
        binding.emptyStateMessage.text = message
    }

    private fun showNoInternet() {
        binding.loadingState.isVisible = false
        binding.list.isVisible = false
        binding.emptyState.isVisible = false
        binding.noInternetState.isVisible = true
        binding.btnRetry.setOnClickListener { loadUsers() }
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