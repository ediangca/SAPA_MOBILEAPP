package com.ddn.peedo.project.sapa.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout

fun createDot(context: Context, schoolId: String): View {
    return View(context).apply {
        layoutParams = LinearLayout.LayoutParams(10, 10).apply {
            marginEnd = 6
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(SchoolColorProvider.getColor(context, schoolId))
        }
    }
}
