package com.ddn.peedo.project.sapa.ui.dashboard.ui.school

import android.annotation.SuppressLint
import android.app.Dialog
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
import com.ddn.peedo.project.sapa.databinding.FragmentSchoolBinding
import com.ddn.peedo.project.sapa.model.School
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.adapter.SchoolAdapter
import com.ddn.peedo.project.sapa.databinding.ItemSchoolStudentBinding
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import kotlinx.coroutines.launch
import android.R
import android.animation.ValueAnimator
import android.view.MotionEvent
import androidx.core.widget.addTextChangedListener
import com.ddn.peedo.project.sapa.adapter.StudentAdapter
import com.ddn.peedo.project.sapa.model.VwUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


class SchoolFragment : Fragment() {

    private var _binding: FragmentSchoolBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var status =
        arrayOf(
            "ALL",
            "PENDING",
            "APPROVED",
            "INACTIVE",
            "SUSPENDED",
        )
    private var selectedItem: String = ""

    private var list: ArrayList<School> = ArrayList()
    private lateinit var adapter: SchoolAdapter

    // Full (unfiltered) student list for the currently open dialog.
    // Populated after a successful fetch in showStudentsDialog(); applyFilters()
    // filters against this, never against the adapter's current (possibly already
    // filtered) list.
    private var currentSchoolStudents: List<VwUser> = emptyList()

    private var isSheetExpanded = false
    private var collapsedSheetHeight = 0

    private var currentSearchQuery: String = ""
    private var searchDebounceJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val schoolViewModel =
            ViewModelProvider(this)[SchoolViewModel::class.java]

        _binding = FragmentSchoolBinding.inflate(inflater, container, false)
        return binding.root


    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initComponent()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initComponent() {

        var statusAdapter =
            context?.let { ArrayAdapter(it, R.layout.simple_spinner_dropdown_item, status) }


        with(binding.spinnerStatus)
        {
            adapter = statusAdapter
            setSelection(0, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    this@SchoolFragment.selectedItem =
                        parent.getItemAtPosition(position).toString() // Get the selected item

                    Log.d(
                        "SchoolFragment_INFO",
                        "Selected Item: " + this@SchoolFragment.selectedItem
                    )
                    showSchool(this@SchoolFragment.selectedItem);

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Optionally handle the case where no item is selected

                }
            }

            prompt = "Select Status"
            gravity = Gravity.CENTER

            initRecycler()
            loadSchools()
        }
        binding.swipeRefresh.setOnRefreshListener {

            binding.spinnerStatus.setSelection(0, false)
            loadSchools()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initRecycler() {
        adapter = SchoolAdapter(emptyList()) { school ->
            showStudentsDialog(school)
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
    }

    private fun loadSchools() {
        showLoading()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.create(requireContext()).getSchools()

                hideLoading()
                binding.swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    list = ArrayList(response.body() ?: emptyList())
                    adapter.updateData(list)
                    Log.d(
                        "SchoolFragment_INFO",
                        "Success: " + response.body()
                    )
                    updateEmptyState(list)
                } else {
                    showEmptyState()
                    Log.d(
                        "SchoolFragment_INFO",
                        "Error: " + response.message()
                    )
                    hideLoading()
                    binding.swipeRefresh.isRefreshing = false
                    showNoInternetState()
                }

            } catch (e: Exception) {
                Log.d(
                    "SchoolFragment_INFO",
                    "Error: " + e.message
                )
                hideLoading()
                binding.swipeRefresh.isRefreshing = false
                showNoInternetState()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSchool(status: String) {

        if (status == "ALL") {
            adapter.updateData(list)
            updateEmptyState(list)
            adapter.notifyDataSetChanged()
            return
        }

        val filtered = list.filter {
            mapStatus(it.status) == status
        }

        adapter.updateData(filtered)
        updateEmptyState(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun mapStatus(status: Int?): String {
        return when (status) {
            1 -> "APPROVED"
            0 -> "PENDING"
            2 -> "INACTIVE"
            3 -> "SUSPENDED"
            else -> "UNKNOWN"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showStudentsDialog(school: School) {
        currentSearchQuery = ""
        currentSchoolStudents = emptyList()
        searchDebounceJob?.cancel()

        val dialogBinding = ItemSchoolStudentBinding.inflate(layoutInflater)

        val dialog =
            Dialog(requireContext(), com.ddn.peedo.project.sapa.R.style.BottomDialogStyle).apply {
                setContentView(dialogBinding.root)
                setCancelable(true)
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            }

        val studentAdapter = StudentAdapter(
            emptyList(),
            requireContext(),
            lifecycleOwner = viewLifecycleOwner
        )

        dialogBinding.studentList.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.studentList.adapter = studentAdapter

        // Reuse the header text field to show the school name instead of a date
        dialogBinding.txtSchoolName.text = school.schoolName
        dialogBinding.txtAddress.text = school.address
        dialogBinding.etSearch.setText("")

        setupSearch(dialogBinding, studentAdapter)

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
        showDialogLoading(dialogBinding)

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.api(requireContext())

                val schoolID = school.schoolID
                if (schoolID.isNullOrEmpty()) {
                    showDialogEmpty(dialogBinding)
                    return@launch
                }

                val response = api.getStudentsBySchoolID(schoolID)

                if (!dialog.isShowing) return@launch

                if (response.isSuccessful && response.body() != null) {
                    val fetchedStudents = response.body()!!
                    currentSchoolStudents = fetchedStudents

                    if (fetchedStudents.isEmpty()) {
                        showDialogEmpty(dialogBinding)
                    } else {
                        studentAdapter.updateData(fetchedStudents)
                        showDialogList(dialogBinding)
                    }
                } else {
                    Log.e(
                        "SchoolFragment_INFO",
                        "Failed to fetch students for school ${school.schoolID}"
                    )
                    showDialogEmpty(dialogBinding)
                }
            } catch (e: Exception) {
                Log.e("SchoolFragment_INFO", "Error loading students dialog", e)
                if (dialog.isShowing) showDialogEmpty(dialogBinding)
            }
        }
    }

    private fun setupSearch(binding: ItemSchoolStudentBinding, studentAdapter: StudentAdapter) {
        binding.etSearch.addTextChangedListener { editable ->
            searchDebounceJob?.cancel()
            searchDebounceJob = lifecycleScope.launch {
                delay(300)
                currentSearchQuery = editable?.toString().orEmpty().trim()
                applyFilters(binding, studentAdapter)
            }
        }
    }


    private fun applyFilters(binding: ItemSchoolStudentBinding, studentAdapter: StudentAdapter) {
        var filtered = currentSchoolStudents

        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.fullname.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            showDialogEmpty(
                binding,
                if (currentSearchQuery.isNotEmpty()) "No student found matching \"$currentSearchQuery\""
                else "No students found"
            )
        } else {
            showDialogList(binding)
            studentAdapter.updateData(filtered)
        }
    }

    private fun expandSheet(binding: ItemSchoolStudentBinding, sheet: View) {
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
        binding.studentList.layoutParams = binding.studentList.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun collapseSheet(binding: ItemSchoolStudentBinding, sheet: View) {
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

        binding.studentList.layoutParams = binding.studentList.layoutParams.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
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

    private fun updateEmptyState(data: List<School>) {
        Log.d("SchoolFragment_INFO", "data.isEmpty(): ${data.isEmpty()}")
        if (data.isEmpty()) {
            showEmptyState()
        } else {
            binding.list.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.noInternetState.visibility = View.GONE
        }
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
            loadSchools()
        }
    }


    private fun showDialogLoading(binding: ItemSchoolStudentBinding) {
        binding.progressLoading.visibility = View.VISIBLE
        binding.studentList.visibility = View.GONE
        binding.dialogEmptyState.visibility = View.GONE
    }

    private fun showDialogList(binding: ItemSchoolStudentBinding) {
        binding.progressLoading.visibility = View.GONE
        binding.studentList.visibility = View.VISIBLE
        binding.dialogEmptyState.visibility = View.GONE
    }

    private fun showDialogEmpty(binding: ItemSchoolStudentBinding, message: String? = null) {
        binding.progressLoading.visibility = View.GONE
        binding.studentList.visibility = View.GONE
        binding.dialogEmptyState.visibility = View.VISIBLE

        binding.emptyStateMessage.text = if (message.isNullOrEmpty()) {
            "No student found."
        } else {
            message
        }
    }


}