package com.ddn.peedo.project.sapa.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.dataclass.CalendarDay
import com.ddn.peedo.project.sapa.utils.createDot
import java.time.LocalDate

class CalendarAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayVH>() {

    private val days = mutableListOf<CalendarDay>()

    fun submit(list: List<CalendarDay>) {
        days.clear()
        days.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayVH(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: DayVH, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    inner class DayVH(view: View) : RecyclerView.ViewHolder(view) {

        private val dayText: TextView = view.findViewById(R.id.txtDay)
        private val dotContainer: LinearLayout = view.findViewById(R.id.dotContainer)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(day: CalendarDay) {

            dayText.text = day.date.dayOfMonth.toString()
            dayText.alpha = if (day.isCurrentMonth) 1f else 0.3f

            when {
                day.isSelected -> {
                    itemView.setBackgroundResource(R.drawable.bg_calendar_selected)
                    dayText.setTextColor(itemView.context.getColor(R.color.white))
                }
                day.isToday -> {
                    itemView.setBackgroundResource(R.drawable.bg_calendar_today)
                    dayText.setTextColor(itemView.context.getColor(R.color.primary))
                }
                else -> {
                    itemView.background = null
                    dayText.setTextColor(itemView.context.getColor(R.color.primary))
                }
            }

            dotContainer.removeAllViews()
            day.schoolIds.forEach {
                dotContainer.addView(createDot(itemView.context, it))
            }

            itemView.setOnClickListener {
                if (day.isCurrentMonth) onDayClick(day.date)
            }
        }

    }
}
