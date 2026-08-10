package com.ddn.peedo.project.sapa.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.model.VwAppointedStudent
import com.ddn.peedo.project.sapa.databinding.ItemStudentBinding
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.utils.SweetAlertUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.databinding.ItemSchoolStudentBinding
import com.ddn.peedo.project.sapa.model.VwUser
class StudentAdapter(
    private var students: List<VwUser>,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(
        val binding: ItemStudentBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        with(holder.binding) {
            txtStudentName.text = student.fullname
            txtStudentEmail.text = student.email

            txtStatus.text = mapStatus(student.status)
            txtStatus.backgroundTintList =
                ContextCompat.getColorStateList(context, statusColor(student.status))

        }
    }


    private fun mapStatus(status: Char?): String {
        return when (status) {
            'A' -> "APPROVED"
            'P' -> "PENDING"
            'I' -> "INACTIVE"
            'S' -> "SUSPENDED"
            'U' -> "UNVERIFIED"
            else -> "UNKNOWN"
        }
    }

    private fun statusColor(status: Char?): Int {
        return when (status) {
            'A' -> R.color.status_approved
            'P' -> R.color.status_pending_bg
            'I' -> R.color.status_inactive
            'S' -> R.color.status_suspended
            else -> R.color.status_unknown
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateData(newList: List<VwUser>) {
        students = newList
        notifyDataSetChanged()
    }
}