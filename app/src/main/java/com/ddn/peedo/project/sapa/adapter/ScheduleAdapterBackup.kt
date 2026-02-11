package com.ddn.peedo.project.sapa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.databinding.ItemScheduleBinding
import com.ddn.peedo.project.sapa.model.VwSlot

class ScheduleAdapterBackup(
    private var schedules: List<VwSlot>
) : RecyclerView.Adapter<ScheduleAdapterBackup.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(
        val binding: ItemScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]

        holder.binding.tvSchoolName.text = "${schedule.dateSlot} - (${schedule.schoolName})"
        holder.binding.tvHospitalName.text = schedule.hospitalName
    }

    override fun getItemCount(): Int = schedules.size

    fun updateData(newList: List<VwSlot>) {
        schedules = newList
        notifyDataSetChanged()
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
}
