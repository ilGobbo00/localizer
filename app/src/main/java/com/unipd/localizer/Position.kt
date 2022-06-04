package com.unipd.localizer

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.room.Room
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.unipd.localizer.BackgroundLocation.Companion.BACKGROUND_SERVICE
import com.unipd.localizer.MainActivity.Companion.PERMISSIONS
import com.unipd.localizer.MainActivity.Companion.SERVICE_RUNNING
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil


class Position : Fragment() {
    // Database variables/values
    private lateinit var database: LocationsDatabase
    private lateinit var dbManager: LocationDao

    // Navigation buttons
    private lateinit var historyButton: TextView
    private lateinit var graphButton: TextView

    // Flag used to start/stop requestLocationUpdates
    private var switchingTabs = false
    private var orientationChanged = false

    // Background service button
    private lateinit var backgroundButton: FloatingActionButton

    // Variables for position
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var currentLocation: SimpleLocationItem
    private var latitudeField: TextView? = null
    private var longitudeField: TextView? = null
    private var altitudeField: TextView? = null

    // Variable to enable background service
    private var backgroundService = false

    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

//    private var permissionObtained = false
//    private var locationsReading = false

    // Const values for persistentState and number of locations stored
    companion object {
        const val OLDEST_DATA = 1000 * 60 * 5                                       // Oldest Position: 5min old (millis)(considering 1 location/s)
        const val REFRESH_TIME = 1000                                               // Read position every 1s
        val MAX_SIZE = ceil((OLDEST_DATA / REFRESH_TIME).toDouble())                // Max queue size

        const val BACKGROUND_RUNNING = "background_active"
//        const val SWITCHING_TAB = "switching_tab"
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        Log.i("Localizer/P", "OnCreateView")
        val view = inflater.inflate(R.layout.position_page, container, false)

        // Reference to database repository
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
        persistentState = requireActivity().getPreferences(MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()


        // Find buttons references
        historyButton = view.findViewById(R.id.history_button)
        graphButton = view.findViewById(R.id.graph_button)
        backgroundButton = view.findViewById(R.id.start_stop_background_service)

        // Update FAB based on background service status
        updateFAB()

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
                backgroundService = true
                backgroundButton.setImageResource(R.drawable.stop)
                backgroundButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
                backgroundButton.tag = getString(R.string.running_service)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.starting_background_service),
                    LENGTH_SHORT
                ).show()
            } else {
                // Stop background service
                Log.i("Localizer/P", "Request to STOP background service")
                persistentStateEditor.putBoolean(BACKGROUND_RUNNING, false)
                persistentStateEditor.apply()
                backgroundService = false
                backgroundButton.setImageResource(R.drawable.start)
                backgroundButton.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.teal_200))
                backgroundButton.tag = getString(R.string.service_not_running)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.stopping_background_service),
                    LENGTH_SHORT
                ).show()
            }
        }


        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        } catch (e: IllegalStateException) {
            Log.d("Localizer/P", "Can't create fusedLocationClient due to context error")
            Snackbar.make(view, getString(R.string.error, ""), LENGTH_LONG).show()
            return view
        }

        latitudeField = view.findViewById(R.id.latitude_field)
        longitudeField = view.findViewById(R.id.longitude_field)
        altitudeField = view.findViewById(R.id.altitude_field)

        if (savedInstanceState != null) {
            latitudeField?.text = savedInstanceState.getString("latitude")
            longitudeField?.text = savedInstanceState.getString("longitude")
            altitudeField?.text = savedInstanceState.getString("altitude")
        }

//        val permissionObtained = persistentState.getBoolean(PERMISSIONS, false)

//        Log.i("Localizer/P", "permissionObtained: $permissionObtained")
//        if (!permissionObtained)
//            if(getView() != null)
//                Snackbar.make(requireView(), R.string.permissions_denied, LENGTH_LONG).show()
//            return view


/*        val locationPermissionRequest =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissionObtained = true
                for (permission in permissions) {
                    Log.i("Localizer/Permissions", "Checking: $permission")
                    permissionObtained = permissionObtained && permission.value
                }
                Log.i("Localizer/Permissions", "Checking permissions..")
                if (permissionObtained) {
                    Log.i("Localizer/Permissions", "Permission obtained")
*/

        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = REFRESH_TIME.toLong()

        Log.i("Localizer/P","Creating locationCallback")
        locationCallback = object : LocationCallback() {
            // Called when device location information is available.
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation = locationResult.lastLocation
                currentLocation = SimpleLocationItem(
                    lastLocation.latitude,
                    lastLocation.longitude,
                    lastLocation.altitude
                )
                latitudeField?.text = currentLocation.latitude.toString()             // Display data if a location is available
                longitudeField?.text = currentLocation.longitude.toString()
                altitudeField?.text = currentLocation.altitude.toString()

                runBlocking {
                    launch {
                        val entry =
                            LocationEntity(lastLocation.time, currentLocation)
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
                        val locationList = dbManager.getAllLocations()
                        if (locationList.size > MAX_SIZE)
                            dbManager.deleteOld()
                    }
                }
                super.onLocationResult(locationResult)
            }
        }
//        if(permissionObtained)
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireContext().mainLooper)
//        else
//            Snackbar.make(requireView(), R.string.permissions_denied, LENGTH_LONG).show()


/*                }
            }


        if(!permissionObtained) {
            Log.i("Localizer/Permissions", "Asking permissions")
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                locationPermissionRequest.launch(arrayOf(Manifest.permission.FOREGROUND_SERVICE))
        }else
            Log.i("Localizer/Permissions", "Permissions already got")
*/

        return view
    }

//    @SuppressLint("MissingPermission")
    @SuppressLint("MissingPermission")
    override fun onResume() {
        Log.i("Localizer/P", "onResume")


        if(!persistentState.getBoolean(PERMISSIONS, false))
            // Displayed if the user select to denied permissions in permissions window
            Snackbar.make(requireView(), R.string.permissions_denied, LENGTH_LONG).show()
        else{
            if(!persistentState.getBoolean(SERVICE_RUNNING, false)){
                Log.i("Localizer/P", "Dentro locationReading")
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireContext().mainLooper)
                persistentStateEditor.putBoolean(SERVICE_RUNNING, true)
                persistentStateEditor.apply()
            }
        }
//        if(persistentState.getBoolean(PERMISSIONS, false) && /*this::fusedLocationClient.isInitialized && */this::locationRequest.isInitialized/* && this::locationCallback.isInitialized*/)
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,requireContext().mainLooper)
        super.onResume()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putString("latitude", latitudeField?.text.toString())
        savedInstanceState.putString("longitude", longitudeField?.text.toString())
        savedInstanceState.putString("altitude", altitudeField?.text.toString())
        super.onSaveInstanceState(savedInstanceState)
    }

    private fun updateFAB() {
        Log.d("Localizer/P", "updateFAB")
        Log.d("Localizer/P", "Persistent state: $persistentState")
        if (!persistentState.getBoolean(BACKGROUND_RUNNING, false))
            return
        Log.d("Localizer/P", "Persistent state data found")
        backgroundService = true
        backgroundButton.setImageResource(R.drawable.stop)
        backgroundButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
        backgroundButton.tag = getString(R.string.running_service)
    }

    @SuppressLint("MissingPermission")  // Already checked in Activity lifecycle
    override fun onStart() {
        Log.i("Localizer/P", "onStart")
//        permissionObtained = persistentState.getBoolean(PERMISSIONS, false)
//        Log.i("Localizer/P", "permissionObtained: $permissionObtained")

        // Pause background service when the position fragment is displayed. Resume later
        val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
        backgroundIntent.putExtra(BACKGROUND_SERVICE, false)
        requireContext().stopService(backgroundIntent)

        // If the app has permissions, when opened start reading locations
        if(persistentState.getBoolean(PERMISSIONS, false)){
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireContext().mainLooper)
            persistentStateEditor.putBoolean(SERVICE_RUNNING, true)
        }else
            persistentStateEditor.putBoolean(SERVICE_RUNNING, false)

        persistentStateEditor.apply()



        super.onStart()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationChanged = true
    }

    override fun onStop() {
        Log.d("Localizer/Lifecycle", "OnPause")
        if(this::locationCallback.isInitialized && !orientationChanged)
            fusedLocationClient.removeLocationUpdates(locationCallback)


        if(switchingTabs || (!switchingTabs && backgroundService)) {

            // If user enable background service start it
            Log.d("Localizer/Lifecycle", "OnPause --> try to start service")
            val backgroundIntent =
                Intent(activity?.applicationContext, BackgroundLocation::class.java)
            backgroundIntent.putExtra(BACKGROUND_SERVICE, true)
            backgroundIntent.putExtra(PERMISSIONS, persistentState.getBoolean(PERMISSIONS, false))
            requireContext().startService(backgroundIntent)
            //onPaused caused by exiting the app
            // Stop the current position updating
//            if(this::locationCallback.isInitialized)
//                fusedLocationClient.removeLocationUpdates(locationCallback)
        }

            /* If user enable background service start it
            if (backgroundService) {
                Log.d("Localizer/Lifecycle", "OnPause --> try to start service")
                val backgroundIntent =
                    Intent(activity?.applicationContext, BackgroundLocation::class.java)
                backgroundIntent.putExtra(BACKGROUND_SERVICE, true)
                backgroundIntent.putExtra(PERMISSIONS, persistentState.getBoolean(PERMISSIONS, false))
                requireContext().startService(backgroundIntent)
            }
        }else{
             If user enable background service start it
            if (backgroundService) {
                Log.d("Localizer/Lifecycle", "OnPause --> try to start service")
                val backgroundIntent =
                    Intent(activity?.applicationContext, BackgroundLocation::class.java)
                backgroundIntent.putExtra(BACKGROUND_SERVICE, true)
                backgroundIntent.putExtra(PERMISSIONS, persistentState.getBoolean(PERMISSIONS, false))
                requireContext().startService(backgroundIntent)
        }
         else current position updating can continue*/




        super.onStop()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//    }
}















