package com.ddn.peedo.project.sapa.ui.dashboard.ui.school

import android.R
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
import com.ddn.peedo.project.sapa.databinding.FragmentSchoolBinding
import com.ddn.peedo.project.sapa.model.School

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.ddn.peedo.project.sapa.adapter.SchoolAdapter
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import kotlinx.coroutines.launch


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

    private fun initRecycler() {
        adapter = SchoolAdapter(emptyList())
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
                        "Success: " + response.body() )
                    updateEmptyState(list)
                } else {
                    showEmptyState()
                    Log.d(
                        "SchoolFragment_INFO",
                        "Error: " + response.message() )
                    hideLoading()
                    binding.swipeRefresh.isRefreshing = false
                    showNoInternetState()
                }

            } catch (e: Exception) {
                Log.d(
                    "SchoolFragment_INFO",
                    "Error: " + e.message )
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



}