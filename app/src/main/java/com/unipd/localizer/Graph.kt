package com.unipd.localizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

class Graph : Fragment(){
    private lateinit var  historyButton: TextView
    private lateinit var  positionButton: TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.graph_page, container, false)
        //view.setContentView(R.layout.position_page)

        historyButton = view.findViewById(R.id.history_button)
        positionButton = view.findViewById(R.id.position_button)

        historyButton.setOnClickListener { v ->
            val destinationTab = GraphDirections.actionGraphPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        positionButton.setOnClickListener { v ->
            val destinationTab = GraphDirections.actionGraphPageToPositionPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        return view
    }
}