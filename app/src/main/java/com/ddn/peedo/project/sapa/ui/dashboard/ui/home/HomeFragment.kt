package com.ddn.peedo.project.sapa.ui.dashboard.ui.home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ddn.peedo.project.sapa.databinding.FragmentHomeBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initComponent()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initComponent() {
        with(binding) {
            val today = LocalDate.now()

            val formatter = DateTimeFormatter.ofPattern(
                "EEEE, MMM dd, yyyy",
                Locale.ENGLISH
            )

            dateText.text = today.format(formatter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}