package com.ddn.peedo.project.sapa.ui.dashboard.ui.users

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.databinding.ItemUserBinding
import com.ddn.peedo.project.sapa.model.VwUser
import com.ddn.peedo.project.sapa.utils.UserStatusUtil

class UserAdapter(
    private val listener: UserActionListener
) : ListAdapter<VwUser, UserAdapter.UserViewHolder>(DIFF_CALLBACK) {

    interface UserActionListener {
        fun onApprove(user: VwUser)
        fun onResendVerification(user: VwUser)
        fun onItemClick(user: VwUser) {} // optional, default no-op
    }

    // Tracks userIDs currently awaiting a network response
    private val loadingUserIds = mutableSetOf<String>()

    fun setLoading(userId: String, isLoading: Boolean) {
        if (isLoading) loadingUserIds.add(userId) else loadingUserIds.remove(userId)
        val position = currentList.indexOfFirst { it.userID == userId }
        if (position != -1) notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: VwUser) {
            binding.uFullName.text = user.fullname
            binding.uSchool.text = user.schoolName ?: "No school assigned"

            binding.tvStatus.text = UserStatusUtil.label(user.status)
            binding.tvStatus.background.setTint(statusColor(user.status))

            val isLoading = loadingUserIds.contains(user.userID)
            val isUnverified = user.status == UserStatusUtil.UNVERIFIED
            val isApprovable = user.status == UserStatusUtil.PENDING

            binding.actionProgress.isVisible = isLoading
            binding.reverification.isVisible = isUnverified && !isLoading
            binding.approve.isVisible = isApprovable && !isLoading

            binding.reverification.setOnClickListener {
                if (isUnverified && !isLoading) listener.onResendVerification(user)
            }
            binding.approve.setOnClickListener {
                if (isApprovable && !isLoading) listener.onApprove(user)
            }

            binding.root.setOnClickListener { listener.onItemClick(user) }
        }

        private fun statusColor(status: Char?): Int = when (status) {
            UserStatusUtil.APPROVED -> Color.parseColor("#003366")   // primary
            UserStatusUtil.PENDING -> Color.parseColor("#F0A500")    // amber
            UserStatusUtil.UNVERIFIED -> Color.parseColor("#607D8B") // blue-grey
            UserStatusUtil.SUSPENDED -> Color.parseColor("#C62828")  // red
            UserStatusUtil.INACTIVE -> Color.parseColor("#9E9E9E")   // grey
            else -> Color.GRAY
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VwUser>() {
            override fun areItemsTheSame(oldItem: VwUser, newItem: VwUser) =
                oldItem.userID == newItem.userID

            override fun areContentsTheSame(oldItem: VwUser, newItem: VwUser) =
                oldItem == newItem
        }
    }
}