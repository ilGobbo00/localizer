package com.unipd.localizer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Color
import android.location.Location.distanceBetween
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.unipd.localizer.Position.Companion.BACKGROUND_RUNNING
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Graph : Fragment(){
    // Flag used to start/stop requestLocationUpdates
    private var switchingTabs = false
    private var orientationChanged = false

    private lateinit var  positionButton: TextView
    private lateinit var  historyButton: TextView
    private lateinit var  graphButton: TextView

    private lateinit var chartLocations: LineChart
    private lateinit var chartAltitudes: LineChart

    private lateinit var database: LocationsDatabase
    private lateinit var dbManager: LocationDao
    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.graph_page, container, false)
        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(Context.MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

//        database =Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations").build()
        try {
            database = LocationsDatabase.getDatabase(requireContext())
        }catch (e: java.lang.IllegalStateException){
            Log.e("Localizer/P", "Can't create database due to context error")
            Snackbar.make(view, getString(R.string.error, "with database creation"), BaseTransientBottomBar.LENGTH_LONG).show()
            return view
        }
        dbManager = database.locationDao()

        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        //view.setContentView(R.layout.position_page)

        positionButton = view.findViewById(R.id.position_button)
        historyButton = view.findViewById(R.id.history_button)
        graphButton = view.findViewById(R.id.graph_button)
        chartLocations = view.findViewById(R.id.chartLocations)
        chartAltitudes = view.findViewById(R.id.chartAltitudes)
        chartAddStyles(chartLocations)
        chartAddStyles(chartAltitudes)


        switchingTabs = false
        positionButton.setOnClickListener { v ->
            val destinationTab = GraphDirections.actionGraphPageToPositionPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }
        historyButton.setOnClickListener { v ->
            val destinationTab = GraphDirections.actionGraphPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }
        graphButton.setOnClickListener { v ->
            val destinationTab = GraphDirections.actionGraphPageToGraphPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }


        var locationsList: MutableList<LocationEntity> = ArrayList()
        runBlocking {
            launch {
                locationsList = dbManager.getAllLocations() as MutableList<LocationEntity>

            }
        }
        val entriesChart1: MutableList<Entry> = ArrayList()
        val entriesChart2: MutableList<Entry> = ArrayList()
        val distance = FloatArray(1)
        var currentLoc: SimpleLocationItem
        val chart1Title: TextView = view.findViewById(R.id.locations_chart_title)
        val chart2Title: TextView = view.findViewById(R.id.altitude_chart_title)

        if(locationsList.size < 2){
            if(locationsList.isEmpty())
                chart1Title.text = getString(R.string.noData)
            else
                chart1Title.text = getString(R.string.tooFewItems)

            chart2Title.text = ""
            (chartLocations as ViewGroup).removeView(chartLocations)
            (chartAltitudes as ViewGroup).removeView(chartAltitudes)
            return view
        }

        locationsList.reverse()

        val dataSetChart1: LineDataSet
        val dataSetChart2: LineDataSet
        when(resources.configuration.orientation){
           ORIENTATION_PORTRAIT -> {
               var nextLoc: SimpleLocationItem
               for(i in IntRange(0, locationsList.size-2) ) {
                   // Location 1
                   currentLoc = locationsList[i].location
                   // Location 2
                   nextLoc = locationsList[i+1].location

                   distanceBetween(currentLoc.latitude, currentLoc.longitude, nextLoc.latitude, nextLoc.longitude, distance)
                   entriesChart1.add(Entry(i.toFloat(), distance[0]))
                   entriesChart2.add(Entry(i.toFloat(), (/*nextLoc.altitude - */currentLoc.altitude).toFloat() ))
               }
               // Get last entry for altitude chart
               entriesChart2.add(Entry((locationsList.size-1).toFloat(), locationsList[locationsList.size-1].location.altitude.toFloat()))

               dataSetChart1 = LineDataSet(entriesChart1, "Locations trend [m]")
               dataSetChart2 = LineDataSet(entriesChart2, "Altitude trend [m]")
           }
            else -> {
                for(i in IntRange(0, locationsList.size-1)) {
                    currentLoc = locationsList[i].location

                    entriesChart1.add(Entry(i.toFloat(), currentLoc.latitude.toFloat()))
                    entriesChart2.add(Entry(i.toFloat(), currentLoc.longitude.toFloat()))
                }
                // Get last entry for altitude chart
                entriesChart2.add(Entry((locationsList.size-1).toFloat(), locationsList[locationsList.size-1].location.altitude.toFloat()))

                dataSetChart1 = LineDataSet(entriesChart1, "Latitude")
                dataSetChart2 = LineDataSet(entriesChart2, "Longitude")
            }
        }

        dataAddStyles(dataSetChart1)
        dataAddStyles(dataSetChart2)

        val lineDataLocations = LineData(dataSetChart1)
        chartLocations.data = lineDataLocations
        chartLocations.invalidate()

        val lineDataAltitude = LineData(dataSetChart2)
        chartAltitudes.data = lineDataAltitude
        chartAltitudes.invalidate()

        return view
    }

    private fun chartAddStyles(chart: LineChart){
        chart.setBackgroundColor(Color.WHITE)
        chart.description.isEnabled = false
        chart.isDragEnabled = false
        chart.setDrawBorders(true)
        chart.setTouchEnabled(true)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        val xAxis = chart.xAxis
        xAxis.isEnabled = false
    }

    private fun dataAddStyles(data: LineDataSet){
        data.setCircleColor(resources.getColor(R.color.teal_700))
        data.setColors(Color.CYAN)
        data.setDrawCircles(false)
        data.setDrawValues(false)
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
        val destinationTab = GraphDirections.actionGraphPageToGraphPage()
        try {
            Navigation.findNavController(requireView()).navigate(destinationTab)
        }catch (e: IllegalStateException){}
        orientationChanged = true
    }
}