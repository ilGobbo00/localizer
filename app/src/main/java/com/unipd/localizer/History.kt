package com.unipd.localizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

class History : Fragment(){
    private lateinit var  positionButton: TextView
    private lateinit var  graphButton: TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.history_page, container, false)
        //view.setContentView(R.layout.position_page)

        positionButton = view.findViewById(R.id.position_button)
        graphButton = view.findViewById(R.id.graph_button)

        positionButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToPositionPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        graphButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToGraphPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        return view
    }
}