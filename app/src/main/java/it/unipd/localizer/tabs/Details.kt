package it.unipd.localizer.tabs

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
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import it.unipd.localizer.LocationAdapter.Companion.DAY_PATTERN
import it.unipd.localizer.LocationAdapter.Companion.HOUR_PATTERN
import it.unipd.localizer.LocationAdapter.Companion.TIMESTAMP_PATTERN
import it.unipd.localizer.R
import it.unipd.localizer.database.LocationDao
import it.unipd.localizer.database.LocationEntity
import it.unipd.localizer.database.LocationsDatabase
import it.unipd.localizer.service.BackgroundLocation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class Details:Fragment(), OnMapReadyCallback {
    // Flag used to start/stop requestLocationUpdates
    private var orientationChanged = false
    private var backPressed = false

    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    // Button to return to History tab
    private var backButton: FloatingActionButton? = null

    // Database
    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao

    // Labels
    private var time: TextView? = null
    private var latitude: TextView? = null
    private var longitude: TextView? = null
    private var altitude: TextView? = null

    // Google Map reference
    private lateinit var mapFragment: SupportMapFragment

    // Timestamp of location to display
    private lateinit var epochString: String

    // Location to display
    private var locationToDisplay: LocationEntity? = null

    companion object{
        // Constant for persistent state
        const val SHOW_DETAILS = "show_details"
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        persistentState = requireActivity().getPreferences(Context.MODE_PRIVATE)
//        persistentStateEditor = persistentState.edit()
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.location_detail, container, false)

        try {
            database = LocationsDatabase.getDatabase(requireContext())
        }catch (e: IllegalStateException){
            Log.e("Localizer/P", "Can't create database due to context error")
            Snackbar.make(view, getString(R.string.error, "with database creation"), LENGTH_LONG).show()
            return view
        }
        dbManager = database.locationDao()

        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(Context.MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

        // Set background button
        backButton = view?.findViewById(R.id.back_to_list)
        backButton?.setOnClickListener { clickView ->
            val destinationTab = DetailsDirections.actionDetailPageToHistoryPage()
            Navigation.findNavController(clickView).navigate(destinationTab)
            backPressed = true
        }

        // Reference to labels
        time = view?.findViewById(R.id.location_time_detail)
        latitude = view?.findViewById(R.id.location_latitude_detail)
        longitude = view?.findViewById(R.id.location_longitude_detail)
        altitude = view?.findViewById(R.id.location_altitude_detail)

        // Get parameter passed thought fragment.
        // Use the timestamp (unique) to obtain the element from DB after conversion from human-readable date to epoch
        epochString = DetailsArgs.fromBundle(requireArguments()).location
        val timestamp: Long? = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.ITALY).parse(epochString)?.time

        runBlocking{
            // Display loading text
            time?.text = getString(R.string.loading_details)
            latitude?.text = getString(R.string.loading_details)
            longitude?.text = getString(R.string.loading_details)
            altitude?.text = getString(R.string.loading_details)

            launch {
                locationToDisplay = timestamp?.let { dbManager.getLocation(it) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    time?.text = Html.fromHtml(getString(R.string.time_location_detail, SimpleDateFormat(DAY_PATTERN, Locale.ITALY).format(timestamp), SimpleDateFormat(HOUR_PATTERN, Locale.ITALY).format(timestamp)), FROM_HTML_MODE_LEGACY)
                    latitude?.text = Html.fromHtml(getString(R.string.latitude_read_detail, locationToDisplay?.location!!.latitude.toString()), FROM_HTML_MODE_LEGACY)
                    longitude?.text = Html.fromHtml(getString(R.string.longitude_read_detail, locationToDisplay?.location!!.longitude.toString()), FROM_HTML_MODE_LEGACY)
                    altitude?.text = Html.fromHtml(getString(R.string.altitude_read_detail, locationToDisplay?.location!!.altitude.toString()), FROM_HTML_MODE_LEGACY)
                }else{
                    time?.text = getString(R.string.time_location_detail_compat, SimpleDateFormat(DAY_PATTERN, Locale.ITALY).format(timestamp), SimpleDateFormat(HOUR_PATTERN, Locale.ITALY).format(timestamp))
                    latitude?.text = getString(R.string.latitude_read_detail_compat, locationToDisplay?.location!!.latitude.toString())
                    longitude?.text = getString(R.string.longitude_read_detail_compat, locationToDisplay?.location!!.longitude.toString())
                    altitude?.text = getString(R.string.altitude_read_detail_compat, locationToDisplay?.location!!.altitude.toString())
                }
                mapFragment = childFragmentManager.findFragmentByTag("googleMap") as SupportMapFragment

            }

        }
        if(this::mapFragment.isInitialized)
            mapFragment.getMapAsync(this)


        if(locationToDisplay == null){
            val title: TextView = view.findViewById(R.id.title_position_detail)
            val errorDay: String = SimpleDateFormat(DAY_PATTERN, Locale.ITALY).format(timestamp)
            val errorTime: String = SimpleDateFormat(HOUR_PATTERN, Locale.ITALY).format(timestamp)

            title.text = getString(R.string.location_details_title_error)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                time?.text = Html.fromHtml(getString(R.string.invalid_location_detail, errorDay, errorTime), FROM_HTML_MODE_LEGACY)
            else
                time?.text = getString(R.string.invalid_location_detail_compat, errorDay, errorTime)
            latitude?.text = getString(R.string.invalid_location_hint)
            longitude?.text = ""
            altitude?.text = ""
            return view
        }

        return view
    }

    override fun onPause() {
        val backgroundService = persistentState.getBoolean(Position.BACKGROUND_RUNNING, false)
        if(!backPressed && !orientationChanged && !backgroundService) {
            val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
            backgroundIntent.putExtra(BackgroundLocation.BACKGROUND_SERVICE, false)
            requireContext().stopService(backgroundIntent)
        }
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val destinationTab = DetailsDirections.actionDetailPageToDetailPage(epochString)
        try {
            Navigation.findNavController(requireView()).navigate(destinationTab)
        }catch (e: IllegalStateException){}
        orientationChanged = true
    }

    override fun onMapReady(map: GoogleMap) {
        locationToDisplay?.let{
            val location = LatLng(locationToDisplay!!.location.latitude, locationToDisplay!!.location.longitude)
            map.addMarker(MarkerOptions().position(location).title("Location"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

}
