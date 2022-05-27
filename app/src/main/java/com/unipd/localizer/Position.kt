package com.unipd.localizer

//import android.location.Location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.unipd.localizer.BackgroundLocation.Companion.BACKGROUND_SERVICE
import com.unipd.localizer.databinding.PositionPageBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil


class Position : Fragment() {
    // Variable to get view objects
//    private var _binding: PositionPageBinding? = null
//    private val binding get() = _binding!!

    // Database variables/values
//    private lateinit var referenceLocationRepo: ReferenceLocationRepo
    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao


    // Navigation buttons
    private lateinit var historyButton: TextView /*= binding.historyButton.setOnClickListener { v ->
        val destinationTab = PositionDirections.actionPositionPageToHistoryPage()
        Navigation.findNavController(v).navigate(destinationTab)
    }*/

    private lateinit var graphButton: TextView/* = binding.graphButton.setOnClickListener { v ->
        val destinationTab = PositionDirections.actionPositionPageToGraphPage()
        Navigation.findNavController(v).navigate(destinationTab)
    }*/



    // Background service button
    private lateinit var backgroundButton: FloatingActionButton

    // Variables for position
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: SimpleLocationItem? = null
    private var latitudeField: TextView? = null
    private var longitudeField: TextView? = null
    private var altitudeField: TextView? = null

    // Variable for enable background service
    private var backgroundService = false

    // Shared preferences for start/stop background service button
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    // Const values for persistentState and number of locations stored
    companion object{
        const val OLDEST_DATA = 1000 * 60 * 5                                       // Oldest Position: 5min old (millis)(considering 1 location/s)
        const val REFRESH_TIME = 1000                                               // Read position every 1s
        val MAX_SIZE = ceil((OLDEST_DATA / REFRESH_TIME).toDouble())                // Max queue size

        const val BACKGROUND_RUNNING = "background_active"
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        _binding = PositionPageBinding.inflate(inflater, container, false)

        // Reference to database repository
//        referenceLocationRepo = ViewModelProvider(requireActivity()).get(ReferenceLocationRepo::class.java)
        database = Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations").build()
        dbManager = database.locationDao()

        // Get SharedPreferences reference
        persistentState = requireActivity().getPreferences(MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

        val view = inflater.inflate(R.layout.position_page, container, false)
        var permissionObtained = false

        // Find buttons references
//        historyButton = binding.historyButton
//        graphButton = binding.graphButton
//        backgroundButton = binding.startStopBackgroundService
        historyButton = view.findViewById(R.id.history_button)
        graphButton = view.findViewById(R.id.graph_button)
        backgroundButton = view.findViewById(R.id.start_stop_background_service)

        // Update FAB based on background service status
        updateFAB()

        // Set buttons actions
        historyButton.setOnClickListener { v ->
            val destinationTab = PositionDirections.actionPositionPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        graphButton.setOnClickListener { v ->
            val destinationTab = PositionDirections.actionPositionPageToGraphPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        backgroundButton.setOnClickListener { v ->
            if(v.tag == getString(R.string.service_not_running)){
                // Run background service
                Log.i("Localizer/Execution", "Request to RUN background service")
                persistentStateEditor.putBoolean(BACKGROUND_RUNNING, true)
                persistentStateEditor.apply()
                backgroundService = true
                backgroundButton.setImageResource(R.drawable.stop)
                backgroundButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
                backgroundButton.tag = getString(R.string.running_service)
                Toast.makeText(requireContext(), getString(R.string.starting_background_service), LENGTH_SHORT).show()
            }else{
                // Stop background service
                Log.i("Localizer/Execution", "Request to STOP background service")
                persistentStateEditor.putBoolean(BACKGROUND_RUNNING, false)
                persistentStateEditor.apply()
                backgroundService = false
                backgroundButton.setImageResource(R.drawable.start)
                backgroundButton.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.teal_200))
                backgroundButton.tag = getString(R.string.service_not_running)
                Toast.makeText(requireContext(), getString(R.string.stopping_background_service), LENGTH_SHORT).show()
            }
        }


        try{
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        }catch (e: IllegalStateException){
            Log.d("LocationServiceError", "RequireContext returned null")
            return view
        }

//        latitudeField = binding.latitudeField
//        longitudeField = binding.longitudeField
//        altitudeField = binding.altitudeField
        latitudeField = view.findViewById(R.id.latitude_field)
        longitudeField = view.findViewById(R.id.longitude_field)
        altitudeField = view.findViewById(R.id.altitude_field)

        if(savedInstanceState != null){
            latitudeField?.text = savedInstanceState.getString("latitude")
            longitudeField?.text = savedInstanceState.getString("longitude")
            altitudeField?.text = savedInstanceState.getString("altitude")
        }

        // Wait until there are some data. If there are too much elements, delete all data older than OLDEST_DATA
//        referenceLocationRepo.allLocations.observe(
//            requireActivity()
//        ) { locations ->
//            if (locations != null && locations.size >= MAX_SIZE) {
//                Log.i("Localizer/Execution", "Before deleting old data")
//                referenceLocationRepo.deleteOld()
////                Log.d("Execution", "Current ${referenceLocationRepo.allLocations.value?.size} data: ${referenceLocationRepo.allLocations.value}")
//            }
//
//        }

//        Log.d("Execution", "Before locationPermissionRequest")
        val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            //run {
                permissionObtained = true
                for (permission in permissions) {
                    Log.i("Localizer/Permissions", "Checking: $permission")
                    permissionObtained = permissionObtained && permission.value
                }
                Log.i("Localizer/Permissions", "Checking permissions..")
                  if(permissionObtained){
                      Log.i("Localizer/Permissions", "Permission obtained")
                      val locationRequest: LocationRequest = LocationRequest.create()
                      locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                      locationRequest.interval = REFRESH_TIME.toLong()


                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            object : LocationCallback() {
                                // Called when device location information is available.
                                override fun onLocationResult(locationResult : LocationResult){
                                    val lastLocation = locationResult.lastLocation
                                    currentLocation = SimpleLocationItem(lastLocation.latitude, lastLocation.longitude, lastLocation.altitude)
                                    latitudeField?.text = currentLocation?.latitude.toString()             // Display data if a location is available
                                    longitudeField?.text = currentLocation?.longitude.toString()
                                    altitudeField?.text = currentLocation?.altitude.toString()

                                    runBlocking {
                                        launch{
                                            val entry = LocationEntity(lastLocation.time, currentLocation)
                                            dbManager.insertLocation(entry)
                                            val locationList = dbManager.getAllLocations()
                                            Log.i("Localizer/Execution", "Saving ${locationList.size.plus(1)} - " +
                                                    "${lastLocation.time} | " +
                                                    "${currentLocation?.latitude.toString()} | " +
                                                    "${currentLocation?.longitude.toString()}  | " +
                                                    currentLocation?.altitude.toString())
                                        }
                                    }
                                            // Then insert new valid position
//                                            val entry = LocationEntity(lastLocation.time, currentLocation)
//                                            Log.i("Localizer/Execution", "Saving ${referenceLocationRepo.allLocations.value?.size?.plus(1)} - " +
//                                                    "${lastLocation.time} | " +
//                                                    "${currentLocation?.latitude.toString()} | " +
//                                                    "${currentLocation?.longitude.toString()}  | " +
//                                                    currentLocation?.altitude.toString())
//
////                                            referenceLocationRepo.insert(entry)
//                                            runBlocking {
//                                                launch{
//                                                    dbManager.insertLocation(entry)
//                                                }
//                                            }
//                                        }
                                    super.onLocationResult(locationResult)
                                }
                            },
                            requireContext().mainLooper
                        )
                }
           // }
        }
//        Log.d("Execution", "After locationPermissionRequest: $locationPermissionRequest")
        if(!permissionObtained) {
            Log.i("Localizer/Permissions", "Asking permissions")
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                locationPermissionRequest.launch(arrayOf(Manifest.permission.FOREGROUND_SERVICE))
        }else
            Log.i("Localizer/Permissions", "Permissions already got")
        return view
    }

   override fun onSaveInstanceState(savedInstanceState: Bundle){
        savedInstanceState.putString("latitude", latitudeField?.text.toString())
        savedInstanceState.putString("longitude", longitudeField?.text.toString())
        savedInstanceState.putString("altitude", altitudeField?.text.toString())
        super.onSaveInstanceState(savedInstanceState)
    }

    private fun updateFAB(){
        Log.d("Localizer/Execution", "updateFAB")
        Log.d("Localizer/Execution", "Persistent state: $persistentState")
        if(!persistentState.getBoolean(BACKGROUND_RUNNING, false))
            return
        Log.d("Localizer/Execution", "Persistent state data found")
        backgroundService = true
        backgroundButton.setImageResource(R.drawable.stop)
        backgroundButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
        backgroundButton.tag = getString(R.string.running_service)
    }

    override fun onResume() {
        Log.d("Localizer/Lifecycle", "OnResume")

        // Pause background service when the main app is foreground. Resume later
        val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
        backgroundIntent.putExtra(BACKGROUND_SERVICE, false)
        requireContext().stopService(backgroundIntent)
        super.onResume()
    }

    override fun onPause() {
        Log.d("Localizer/Lifecycle", "OnPause")

        // If user enable background service and it isn't working, start it
        if(backgroundService) {
            Log.d("Localizer/Lifecycle", "OnPause --> try to start service")
            val backgroundIntent = Intent(activity?.applicationContext, BackgroundLocation::class.java)
            backgroundIntent.putExtra(BACKGROUND_SERVICE, true)
            requireContext().startService(backgroundIntent)
        }


        super.onPause()
    }
}















