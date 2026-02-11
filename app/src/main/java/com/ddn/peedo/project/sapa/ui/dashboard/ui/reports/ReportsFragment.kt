package com.ddn.peedo.project.sapa.ui.dashboard.ui.reports

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.ddn.peedo.project.sapa.R
import com.ddn.peedo.project.sapa.databinding.FragmentReportsBinding
import com.ddn.peedo.project.sapa.databinding.FragmentScheduleBinding
import com.ddn.peedo.project.sapa.ui.dashboard.ui.schedule.ScheduleViewModel

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(ReportsViewModel::class.java)

        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textConstruction
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}