package com.unipd.localizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Details:Fragment() {
    lateinit var backButton: FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.location_detail, container, false)
        if(view == null)
            return view
        backButton = view.findViewById(R.id.back_to_list)
        backButton.setOnClickListener { clickView ->
            val destinationTab = DetailsDirections.actionDetailPageToHistoryPage()
            Navigation.findNavController(clickView).navigate(destinationTab)
        }

        // TODO Debug
        val test :TextView = view.findViewById(R.id.time_label)
        test.text = DetailsArgs.fromBundle(requireArguments()).location

        return view
    }
}