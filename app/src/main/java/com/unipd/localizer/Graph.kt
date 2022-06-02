package com.unipd.localizer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.room.Room
import com.github.mikephil.charting.charts.LineChart
import com.unipd.localizer.Position.Companion.BACKGROUND_RUNNING
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Graph : Fragment(){
    // Flag used to start/stop requestLocationUpdates
    private var switchingTabs = false
    private var orientationChanged = false

    private lateinit var  historyButton: TextView
    private lateinit var  positionButton: TextView

    private lateinit var chart: LineChart

    private lateinit var database: LocationsDatabase
    private lateinit var dbManager: LocationDao
    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(Context.MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

        database =
            Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations")
                .build()
        dbManager = database.locationDao()

        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.graph_page, container, false)
        //view.setContentView(R.layout.position_page)

        historyButton = view.findViewById(R.id.history_button)
        positionButton = view.findViewById(R.id.position_button)
        chart = view.findViewById(R.id.chart)

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

        runBlocking {
            launch {
                val locationsList = dbManager.getAllLocations()
                //TODO Implementare il grafico
            }
        }
        return view
    }

    override fun onPause() {
        val backgroundService = persistentState.getBoolean(BACKGROUND_RUNNING, false)
        if(!switchingTabs && !backgroundService && !orientationChanged) {
            val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
            backgroundIntent.putExtra(BackgroundLocation.BACKGROUND_SERVICE, false)
            requireContext().stopService(backgroundIntent)
        }

        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationChanged = true
    }
}