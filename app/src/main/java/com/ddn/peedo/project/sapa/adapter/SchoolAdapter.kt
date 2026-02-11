package com.ddn.peedo.project.sapa.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.databinding.ItemSchoolBinding
import com.ddn.peedo.project.sapa.model.School

class SchoolAdapter(
    private var schools: List<School>
) : RecyclerView.Adapter<SchoolAdapter.SchoolViewHolder>() {

    inner class SchoolViewHolder(
        val binding: ItemSchoolBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchoolViewHolder {
        val binding = ItemSchoolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SchoolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SchoolViewHolder, position: Int) {
        val school = schools[position]

        holder.binding.tvSchoolName.text = school.schoolName
        holder.binding.tvAddress.text = school.address
        holder.binding.tvStatus.text = mapStatus(school.status)
    }

    override fun getItemCount(): Int = schools.size

    fun updateData(newList: List<School>) {
        schools = newList
        notifyDataSetChanged()
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
}
