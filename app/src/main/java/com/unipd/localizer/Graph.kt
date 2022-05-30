package com.unipd.localizer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.unipd.localizer.Position.Companion.BACKGROUND_RUNNING

class Graph : Fragment(){
    private var switchingTabs = false
    private lateinit var  historyButton: TextView
    private lateinit var  positionButton: TextView

    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(Context.MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()
        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.graph_page, container, false)
        //view.setContentView(R.layout.position_page)

        historyButton = view.findViewById(R.id.history_button)
        positionButton = view.findViewById(R.id.position_button)

        switchingTabs = false
        historyButton.setOnClickListener { v ->
            val destinationTab = GraphDirections.actionGraphPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }

        positionButton.setOnClickListener { v ->
            val destinationTab = GraphDirections.actionGraphPageToPositionPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }

        return view
    }

    override fun onPause() {
        val backgroundService = persistentState.getBoolean(BACKGROUND_RUNNING, false)
        if(!switchingTabs && !backgroundService) {
            val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
            backgroundIntent.putExtra(BackgroundLocation.BACKGROUND_SERVICE, false)
            requireContext().stopService(backgroundIntent)
        }

        super.onPause()
    }
}