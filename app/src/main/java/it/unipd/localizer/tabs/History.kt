package it.unipd.localizer.tabs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.unipd.localizer.*
import it.unipd.localizer.database.LocationDao
import it.unipd.localizer.LocationAdapter
import it.unipd.localizer.database.LocationEntity
import it.unipd.localizer.database.LocationsDatabase
import it.unipd.localizer.service.BackgroundLocation
import it.unipd.localizer.tabs.Details.Companion.SHOW_DETAILS
import it.unipd.localizer.tabs.Position.Companion.BACKGROUND_RUNNING
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class History : Fragment(), OnMapReadyCallback {
    // Flag used to start/stop requestLocationUpdates
    private var switchingTabs = false
    private var orientationChanged = false

    private lateinit var  positionButton: TextView
    private lateinit var  historyButton: TextView
    private lateinit var  graphButton: TextView
    private lateinit var  deleteButton: FloatingActionButton

    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao

    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var allLocations : List<LocationEntity>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.history_page, container, false)

//        database = Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations").build()
        try {
            database = LocationsDatabase.getDatabase(requireContext())
        }catch (e: java.lang.IllegalStateException){
            Log.e("Localizer/P", "Can't create database due to context error")
            Snackbar.make(view, getString(R.string.error, "with database creation"), LENGTH_LONG).show()
            return view
        }
        dbManager = database.locationDao()

        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(Context.MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()


        positionButton = view.findViewById(R.id.position_button)
        historyButton = view.findViewById(R.id.history_button)
        graphButton = view.findViewById(R.id.graph_button)
        deleteButton = view.findViewById(R.id.delete_all_locations)

        switchingTabs = false
        positionButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToPositionPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }

        historyButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToHistoryPage()
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
            switchingTabs = true
        }

        val elementNum: TextView? = view?.findViewById(R.id.number_element_label)
        val recyclerView: RecyclerView = view.findViewById(R.id.locations_list)


        runBlocking {
            elementNum?.text = getString(R.string.loading_details)
            launch{
                allLocations = dbManager.getAllLocations()
                recyclerView.adapter = LocationAdapter(allLocations, activity)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    elementNum?.text = Html.fromHtml(getString(R.string.num_of_element, allLocations.size), FROM_HTML_MODE_LEGACY)
                else
                    elementNum?.text = getString(R.string.num_of_element_compat, allLocations.size)

                if(resources.configuration.orientation == ORIENTATION_LANDSCAPE)
                    mapFragment = childFragmentManager.findFragmentByTag("googleMap") as SupportMapFragment
            }
        }

        if(this::mapFragment.isInitialized)
            mapFragment.getMapAsync(this)

        return view
    }

    override fun onPause() {
        Log.d("Localizer/H", "onPause, orientationChanged = $orientationChanged")
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
        Log.d("Localizer/H", "onResume, orientationChanged = $orientationChanged")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val destinationTab = HistoryDirections.actionHistoryPageToHistoryPage()
        try {
            Navigation.findNavController(requireView()).navigate(destinationTab)
        }catch (e: IllegalStateException){}
        orientationChanged = true
        Log.d("Localizer/H", "Orientation changed ($orientationChanged)")
    }

    override fun onMapReady(map: GoogleMap) {
        var location: LatLng
        var avgLat = 0.0
        var avgLong = 0.0
        for(l in allLocations){
            avgLat += l.location.latitude
            avgLong += l.location.longitude
            location = LatLng(l.location.latitude, l.location.longitude)
            map.addMarker(MarkerOptions().position(location).title("Location n.${allLocations.indexOf(l)}"))
        }
        if(allLocations.isNotEmpty()){
            location = LatLng(avgLat/allLocations.size, avgLong/allLocations.size)
            val cameraPosition = CameraPosition.Builder()
                .target(location)
                .zoom(10f)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 800, null)
        }
    }
}