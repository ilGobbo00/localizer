package com.unipd.localizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

class Position : Fragment() {
    private lateinit var  historyButton: TextView
    private lateinit var  graphButton: TextView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.position_page, container, false)
        //view.setContentView(R.layout.position_page)

        historyButton = view.findViewById(R.id.history_button)
        graphButton = view.findViewById(R.id.graph_button)

        historyButton.setOnClickListener { v ->
//            Log.e("ClickListener", "History button hit")
            val destinationTab = PositionDirections.actionPositionPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        graphButton.setOnClickListener { v ->
//            Log.e("ClickListener", "Graph button hit")
            val destinationTab = PositionDirections.actionPositionPageToGraphPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        return view
    }
}