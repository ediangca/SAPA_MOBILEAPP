package com.ddn.peedo.project.sapa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.databinding.ItemRecentScheduleBinding
import com.ddn.peedo.project.sapa.model.VwSlot
import java.time.Duration
import java.time.LocalDateTime

class RecentScheduleAdapter(
        private var list: List<VwSlot>
    ) : RecyclerView.Adapter<RecentScheduleAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemRecentScheduleBinding)
            : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRecentScheduleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val item = list[position]

            with(holder.binding) {

                dateSlot.text = item.dateSlot
                tvSchoolName.text = item.schoolName
                tvHospitalName.text = "to ${item.hospitalName} - ${item.sectionName} (${item.shiftName})"

            }
        }

        fun updateData(newList: List<VwSlot>) {
            list = newList
            notifyDataSetChanged()
        }

    }