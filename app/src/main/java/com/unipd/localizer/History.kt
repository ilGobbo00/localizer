package com.unipd.localizer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
//import android.widget.Toast
//import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.unipd.localizer.Details.Companion.SHOW_DETAILS
import com.unipd.localizer.Position.Companion.BACKGROUND_RUNNING
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class History : Fragment(){
    // Flag used to start/stop requestLocationUpdates
    private var switchingTabs = false
    private var orientationChanged = false

    private lateinit var  positionButton: TextView
    private lateinit var  graphButton: TextView
    private lateinit var  deleteButton: FloatingActionButton

    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao

    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        database = Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations").build()
        dbManager = database.locationDao()

        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(Context.MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

        val view = inflater.inflate(R.layout.history_page, container, false)

        positionButton = view.findViewById(R.id.position_button)
        graphButton = view.findViewById(R.id.graph_button)
        deleteButton = view.findViewById(R.id.delete_all_locations)

        switchingTabs = false
        positionButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToPositionPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }

        graphButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToGraphPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }

        deleteButton.setOnClickListener { v ->
            runBlocking {
                launch{
                    dbManager.deleteAll()
                }
            }
            Snackbar.make(v, R.string.elements_deleted, LENGTH_SHORT).show()
            val destinationTab = HistoryDirections.actionHistoryPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        var allLocations : List<LocationEntity>?
        val elementNum: TextView? = view?.findViewById(R.id.number_element_label)

        val recyclerView: RecyclerView = view.findViewById(R.id.locations_list)

        runBlocking {
            elementNum?.text = getString(R.string.loading_details)
            launch{
                allLocations = dbManager.getAllLocations()
                recyclerView.adapter = LocationAdapter(allLocations!!, activity)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    elementNum?.text = Html.fromHtml(getString(R.string.num_of_element, allLocations?.size), FROM_HTML_MODE_LEGACY)
                else
                    elementNum?.text = getString(R.string.num_of_element_compat, allLocations?.size)
            }
        }

        return view
    }

    override fun onPause() {
        Log.d("Localizer/Orientation", "onPause, orientationChanged = $orientationChanged")
        val backgroundService = persistentState.getBoolean(BACKGROUND_RUNNING, false)
        val showDetails = persistentState.getBoolean(SHOW_DETAILS, false)
        if(!switchingTabs && !backgroundService && !orientationChanged && !showDetails) {
            val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
            backgroundIntent.putExtra(BackgroundLocation.BACKGROUND_SERVICE, false)
            requireContext().stopService(backgroundIntent)
        }

        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Log.d("Localizer/Orientation", "orientationChanged = $orientationChanged")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationChanged = true
        Log.d("Localizer/Orientation", "Orientation changed ($orientationChanged)")
    }
}