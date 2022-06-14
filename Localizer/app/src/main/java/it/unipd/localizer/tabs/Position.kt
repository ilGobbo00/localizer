package it.unipd.localizer.tabs

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import it.unipd.localizer.MainActivity.Companion.PERMISSIONS
import it.unipd.localizer.MainActivity.Companion.SERVICE_RUNNING
import it.unipd.localizer.R
import it.unipd.localizer.database.LocationDao
import it.unipd.localizer.database.LocationEntity
import it.unipd.localizer.database.LocationsDatabase
import it.unipd.localizer.database.SimpleLocationItem
import it.unipd.localizer.service.BackgroundLocation
import it.unipd.localizer.service.BackgroundLocation.Companion.BACKGROUND_SERVICE
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Position : Fragment(), NumberPicker.OnValueChangeListener {
    // region Database variables/values
    private lateinit var database: LocationsDatabase
    private lateinit var dbManager: LocationDao
    // endregion

    //region Navigation buttons
    private lateinit var historyButton: TextView
    private lateinit var graphButton: TextView
    //endregion

    //region Flag used to start/stop requestLocationUpdates
    private var switchingTabs = false
    private var orientationChanged = false
    //endregion

    //region Background service button
    private lateinit var backgroundButton: FloatingActionButton
    //endregion

    //region Variables for location methods
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    //endregion

    //region Fields to display data and custom location class
    private lateinit var currentLocation: SimpleLocationItem
    private var latitudeField: TextView? = null
    private var longitudeField: TextView? = null
    private var altitudeField: TextView? = null
    private lateinit var maxNumLabel: TextView
    //endregion

    // Variable to enable background service
    private var backgroundService = false

    //region Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor
    //endregion

    // Number Picker to modify list size
    private lateinit var minuteSelector: NumberPicker

    // Const values for persistentState and number of locations stored
    companion object {
        // Size of the locations list with custom getter method
        var MAX_SIZE = 1
            get() {
                field =  (OLDEST_DATA / REFRESH_TIME)
                return field
            }
        // Milliseconds of the oldest data with custom setter method
        var OLDEST_DATA: Int = 5 * 60 * 1000 // minutes * seconds * milliseconds
            set(value) {
                field = value * 60 * 1000
            }

        const val REFRESH_TIME = 1000                       // Read location every 1s
        // Constant for persistentState
        const val BACKGROUND_RUNNING = "background_active"
        const val MAX_MINUTE = "max_minute"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val ALTITUDE= "altitude"
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView( inflater: LayoutInflater,
                               container: ViewGroup?,
                               savedInstanceState: Bundle? ): View? {
        val view = inflater.inflate(R.layout.position_page, container, false)

        // Reference to database repository
        try {
            database = LocationsDatabase.getDatabase(requireContext())
        }catch (e: java.lang.IllegalStateException){
            Log.e("Localizer/P", "Can't create database due to context error")
            Snackbar.make(view, getString(R.string.error, "with database creation"), LENGTH_LONG).show()
            return view
        }
        dbManager = database.locationDao()

        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

        // Tabs references
        historyButton = view.findViewById(R.id.history_button)
        graphButton = view.findViewById(R.id.graph_button)
        backgroundButton = view.findViewById(R.id.start_stop_background_service)

        // Number picker
        minuteSelector = view.findViewById(R.id.minute_selector)
        minuteSelector.setOnValueChangedListener(this)
        minuteSelector.minValue = 1
        minuteSelector.maxValue = 10
        minuteSelector.value = persistentState.getInt(MAX_MINUTE,5)
        minuteSelector.isClickable

        // Update max stored data age
        OLDEST_DATA = persistentState.getInt(MAX_MINUTE, 5)

        // Update label with current data
        maxNumLabel = view.findViewById(R.id.max_size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            maxNumLabel.text = Html.fromHtml(getString(R.string.max_num_stored, MAX_SIZE), Html.FROM_HTML_MODE_LEGACY)
        else
            maxNumLabel.text = getString(R.string.max_num_stored_comp, MAX_SIZE)

        // Set buttons actions
        switchingTabs = false
        historyButton.setOnClickListener { v ->
            val destinationTab = PositionDirections.actionPositionPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }
        graphButton.setOnClickListener { v ->
            val destinationTab = PositionDirections.actionPositionPageToGraphPage()
            Navigation.findNavController(v).navigate(destinationTab)
            switchingTabs = true
        }
        backgroundButton.setOnClickListener { v ->
            if (v.tag == getString(R.string.service_not_running)) {
                // Run background service
                Log.i("Localizer/P", "Request to RUN background service")
                persistentStateEditor.putBoolean(BACKGROUND_RUNNING, true)
                persistentStateEditor.apply()
                updateFAB()
                Toast.makeText(requireContext(), getString(R.string.starting_background_service), LENGTH_SHORT ).show()
            } else {
                // Stop background service
                Log.i("Localizer/P", "Request to STOP background service")
                persistentStateEditor.putBoolean(BACKGROUND_RUNNING, false)
                persistentStateEditor.apply()
                updateFAB()
                Toast.makeText(requireContext(), getString(R.string.stopping_background_service), LENGTH_SHORT ).show()
            }
        }
        backgroundButton.setOnLongClickListener {
            Toast.makeText(context, getString(R.string.background_button_hint), LENGTH_SHORT)
                .show()
            true
        }

        // Try to create a fusedLocationClient
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        } catch (e: IllegalStateException) {
            Log.d("Localizer/P", "Can't create fusedLocationClient due to context error")
            Snackbar.make(view, getString(R.string.error, ""), LENGTH_LONG).show()
            return view
        }

        // Get reference to location fields
        latitudeField = view.findViewById(R.id.latitude_field)
        longitudeField = view.findViewById(R.id.longitude_field)
        altitudeField = view.findViewById(R.id.altitude_field)

        // Show details of last location read
        if (persistentState.getString(LATITUDE, null) != null) {
            latitudeField?.text = persistentState.getString(LATITUDE, "Invalid data")
            longitudeField?.text = persistentState.getString(LONGITUDE, "Invalid data")
            altitudeField?.text = persistentState.getString(ALTITUDE, "Invalid data")
        }

        // Create a location request for requestLocationUpdates
        locationRequest = LocationRequest.create()
        locationRequest.priority = PRIORITY_HIGH_ACCURACY
        locationRequest.interval = REFRESH_TIME.toLong()

        // Creating callback for requestLocationUpdates with location labels references
        locationCallback = object : LocationCallback() {
            // Called when device location information is available.
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation = locationResult.lastLocation ?: return

                // Create my location object
                currentLocation = SimpleLocationItem(
                    lastLocation.latitude,
                    lastLocation.longitude,
                    lastLocation.altitude
                )

                // Update UI fields
                latitudeField?.text = currentLocation.latitude.toString()
                longitudeField?.text = currentLocation.longitude.toString()
                altitudeField?.text = currentLocation.altitude.toString()

                runBlocking {
                    launch {
                        // Inserting data into DB
                        val entry = LocationEntity(lastLocation.time, currentLocation)
                        dbManager.insertLocation(entry)

                        Log.i(
                            "Localizer/P", "Saving: " +
                                    "${lastLocation.time} | " +
                                    "${currentLocation.latitude} | " +
                                    "${currentLocation.longitude}  | " +
                                    currentLocation.altitude.toString()
                        )
                    }
                    launch {
                        // Check if it's necessary deleting data
                        val locationList = dbManager.getAllLocations()
                        if (locationList.size > MAX_SIZE) {
                            Log.i("Localizer/P", "Deleting oldest data..")
                            dbManager.deleteOld()
                        }
                    }
                }
                super.onLocationResult(locationResult)
            }
        }

        return view
    }

    @SuppressLint("MissingPermission")          // Already checked in Activity lifecycle
    override fun onStart() {
        // Stop foreground service when the position fragment is displayed. It can be resumed later into onStop
        val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
        backgroundIntent.putExtra(BACKGROUND_SERVICE, false)
        requireContext().stopService(backgroundIntent)              // Nothing happened if service isn't running

        persistentStateEditor.apply()

        super.onStart()
    }

    @SuppressLint("MissingPermission")          // Already checked in Activity lifecycle
    override fun onResume() {
        // Check permissions every time fragment starts
        if(persistentState.getBoolean(PERMISSIONS, false)){
            // Needed block to start service after getting permissions
            if(!persistentState.getBoolean(SERVICE_RUNNING, false)){
                Log.i("Localizer/P","requestLocationUpdates onResume")
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireContext().mainLooper)
                persistentStateEditor.putBoolean(SERVICE_RUNNING, true)
            }
        }else{
            Snackbar.make(requireView(), R.string.permissions_denied, LENGTH_LONG).show()
            persistentStateEditor.putBoolean(SERVICE_RUNNING, false)
        }
        persistentStateEditor.apply()

        // Update FAB based on background service status
        updateFAB()

        super.onResume()
    }

    override fun onStop() {
        // Stop location updates from reading position (avoid multiple processes creating a new fragment)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        persistentStateEditor.putBoolean(SERVICE_RUNNING, false)

        // If user change tab displayed or exits after starting "background" service, enable foreground service
        if(switchingTabs || (!switchingTabs && backgroundService)) {
            // If user enable background service start it
            Log.i("Localizer/P", "onStop, start foreground service")
            val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
            backgroundIntent.putExtra(BACKGROUND_SERVICE, true)
            backgroundIntent.putExtra(PERMISSIONS, persistentState.getBoolean(PERMISSIONS, false))
            requireContext().startService(backgroundIntent)
        }

        // If user exit from the app, remove last location data read, else save its fields
        if(!switchingTabs && !orientationChanged){
            persistentStateEditor.remove(LATITUDE)
            persistentStateEditor.remove(LONGITUDE)
            persistentStateEditor.remove(ALTITUDE)
        }else{
            // Save location data for UI labels for when Position will be displayed again
            if(this::currentLocation.isInitialized) {
                persistentStateEditor.putString(LATITUDE, currentLocation.latitude.toString())
                persistentStateEditor.putString(LONGITUDE, currentLocation.longitude.toString())
                persistentStateEditor.putString(ALTITUDE, currentLocation.altitude.toString())
            }
        }

        persistentStateEditor.apply()
        super.onStop()
    }

    // Update fab button based on foreground service status
    private fun updateFAB() {
        if (!persistentState.getBoolean(BACKGROUND_RUNNING, false)){
            backgroundService = false
            backgroundButton.setImageResource(R.drawable.start)
            backgroundButton.backgroundTintList = ColorStateList.valueOf(getColor(resources,R.color.teal_200, null))
            backgroundButton.tag = getString(R.string.service_not_running)
        }else {
            Log.d("Localizer/P", "Persistent state data found")
            backgroundService = true
            backgroundButton.setImageResource(R.drawable.stop)
            backgroundButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
            backgroundButton.tag = getString(R.string.running_service)
        }
    }

    // Function called on phone rotation
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Navigate to the fragment selected autonomously by android
        val destinationTab = PositionDirections.actionPositionPageToPositionPage()
        try {
            Navigation.findNavController(requireView()).navigate(destinationTab)
        }catch (e: java.lang.IllegalStateException){}
        orientationChanged = true
    }

    // Function called on Number Picker scroll
    override fun onValueChange(numPicker: NumberPicker?, old: Int, new: Int) {
        Log.i("Localizer/P", "Oldest minute changed from $old min to $new min")

        // Update maximum stored data age
        OLDEST_DATA = new                               // Custom setter on OLDEST_DATA
        persistentStateEditor.putInt(MAX_MINUTE, new)
        persistentStateEditor.apply()

        // Update UI text
        if(this::maxNumLabel.isInitialized)
            // Custom getter on MAX_SIZE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                maxNumLabel.text = Html.fromHtml(getString(R.string.max_num_stored, MAX_SIZE), Html.FROM_HTML_MODE_LEGACY)
            else
                maxNumLabel.text = getString(R.string.max_num_stored_comp, MAX_SIZE)
    }

}

