package com.ddn.peedo.project.sapa.utils


import android.content.Context
import androidx.core.content.ContextCompat
import com.ddn.peedo.project.sapa.R

object SchoolColorProvider {

    private val colorPool = listOf(
        R.color.hospital_a,
        R.color.hospital_b,
        R.color.hospital_c,
        R.color.hospital_d,
        R.color.hospital_e,
        R.color.hospital_f,
        R.color.hospital_g,
        R.color.hospital_h,
        R.color.hospital_i,
        R.color.hospital_j
    )

    private val map = mutableMapOf<String, Int>()

    fun getColor(context: Context, schoolId: String?): Int {
        val colorRes = map.getOrPut(schoolId as String) {
            colorPool[map.size % colorPool.size]
        }
        return ContextCompat.getColor(context, colorRes)
    }
}
