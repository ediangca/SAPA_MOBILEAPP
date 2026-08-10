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

class StudentAppointedAdapter(
    private var students: List<VwAppointedStudent>,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : RecyclerView.Adapter<StudentAppointedAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(
        val binding: ItemStudentBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.binding.txtStudentName.text = student.fullname
        holder.binding.txtStudentEmail.text = student.email


        Log.d("StudentAdapter_INFO", "Student: $student")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check attendance
                val validateResponse = RetrofitClient.create(context).validateAttendance(student.userID, student.slotID)

                Log.d("ScheduleFragment_INFO", "Scan QR: ${validateResponse.isSuccessful}")
                if (!validateResponse.isSuccessful || validateResponse.body() == null) {
                    SweetAlertUtil.showError(
                        context,
                        "Error",
                        "Unable to validate attendance."
                    )
                    return@launch
                }

                val validation = validateResponse.body()!!

                // 2️⃣ Attendance already exists
                if (validation.hasAttendance) {
                    holder.binding.attendance.setImageResource(R.drawable.vector_check)
                    holder.binding.attendance.imageTintList =
                        ContextCompat.getColorStateList(context, R.color.success)

                } else {
                    holder.binding.attendance.setImageResource(R.drawable.vector_minus)
                    holder.binding.attendance.imageTintList =
                        ContextCompat.getColorStateList(context, R.color.danger)
                }


            } catch (e: Exception) {
                Log.d("ScheduleFragment_INFO", "Error validating attendance", e)
                SweetAlertUtil.showError(
                    context,
                    "Network Error",
                    e.localizedMessage ?: "Something went wrong"
                )
            }
        }
//                                Handler(Looper.getMainLooper()).postDelayed({
//                                    isScanning = false
//                                }, 3000)


    }

    override fun getItemCount(): Int = students.size

    fun updateData(newList: List<VwAppointedStudent>) {
        students = newList
        notifyDataSetChanged()
    }
}
