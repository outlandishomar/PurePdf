package com.outlandishomar.purepdf.ui.open

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.outlandishomar.purepdf.MainActivity
import com.outlandishomar.purepdf.R

class OpenFragment : Fragment() {

    private lateinit var dashboardViewModel: OpenViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(OpenViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_open, container, false)

        val main = activity as MainActivity
        main.openPDFFile()

        main.onBackPressed()

        return root
    }
}