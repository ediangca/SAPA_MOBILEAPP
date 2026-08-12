package com.ddn.peedo.project.sapa.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.databinding.ItemStudentBinding
import com.ddn.peedo.project.sapa.model.VwAppointedStudent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentAppointedAdapter(
    private var students: List<VwAppointedStudent>,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : RecyclerView.Adapter<StudentAppointedAdapter.StudentViewHolder>() {

    private val attendanceDao by lazy {
        SapaDatabase
            .getInstance(context)
            .attendanceDao()
    }

    inner class StudentViewHolder(
        val binding: ItemStudentBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StudentViewHolder {

        val binding =
            ItemStudentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: StudentViewHolder,
        position: Int
    ) {

        val student =
            students[position]

        holder.binding.txtStudentName.text =
            student.fullname

        holder.binding.txtStudentEmail.text =
            student.email

        // -----------------------------------------------------
        // Always reset recycled ViewHolder first
        // -----------------------------------------------------

        holder.binding.attendance.setImageResource(
            R.drawable.vector_close
        )

        holder.binding.attendance.imageTintList =
            ContextCompat.getColorStateList(
                context,
                R.color.danger
            )

        // -----------------------------------------------------
        // Read attendance from Room
        // -----------------------------------------------------

        CoroutineScope(Dispatchers.Main).launch {

            try {

                val hasAttendance =
                    withContext(Dispatchers.IO) {

                        attendanceDao.hasAttendance(
                            slotId = student.slotID,
                            userId = student.userID
                        )
                    }

                if (
                    holder.bindingAdapterPosition ==
                    RecyclerView.NO_POSITION
                ) {
                    return@launch
                }

                if (hasAttendance) {

                    // ✓

                    holder.binding.attendance.setImageResource(
                        R.drawable.vector_check
                    )

                    holder.binding.attendance.imageTintList =
                        ContextCompat.getColorStateList(
                            context,
                            R.color.success
                        )

                } else {

                    // ✕

                    holder.binding.attendance.setImageResource(
                        R.drawable.vector_close
                    )

                    holder.binding.attendance.imageTintList =
                        ContextCompat.getColorStateList(
                            context,
                            R.color.danger
                        )
                }

            } catch (e: Exception) {

                Log.e(
                    "StudentAdapter",
                    "Error reading attendance cache",
                    e
                )
            }
        }
    }

    override fun getItemCount(): Int =
        students.size

    fun updateData(
        newList: List<VwAppointedStudent>
    ) {

        students =
            newList

        notifyDataSetChanged()
    }
}