package com.ddn.peedo.project.sapa.adapter

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.databinding.DialogTodayScheduleBinding
import com.ddn.peedo.project.sapa.databinding.ItemSchoolBinding
import com.ddn.peedo.project.sapa.databinding.ItemSchoolStudentBinding
import com.ddn.peedo.project.sapa.model.School
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate
class SchoolAdapter(
    private var schools: List<School>,
    private val onStudentClick: (School) -> Unit
) : RecyclerView.Adapter<SchoolAdapter.SchoolViewHolder>() {

    inner class SchoolViewHolder(
        val binding: ItemSchoolBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchoolViewHolder {
        val binding = ItemSchoolBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SchoolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SchoolViewHolder, position: Int) {
        val school = schools[position]
        with(holder.binding) {
            tvSchoolName.text = school.schoolName
            tvAddress.text = school.address
            tvStatus.text = mapStatus(school.status)

            tvStudent.setOnClickListener {
                onStudentClick(school)
            }
        }
    }

    override fun getItemCount(): Int = schools.size

    fun updateData(newList: List<School>) {
        schools = newList
        notifyDataSetChanged()
    }

    private fun mapStatus(status: Int?): String = when (status) {
        1 -> "APPROVED"
        0 -> "PENDING"
        2 -> "INACTIVE"
        3 -> "SUSPENDED"
        else -> "UNKNOWN"
    }
}