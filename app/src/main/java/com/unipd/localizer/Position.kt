package com.unipd.localizer

import android.Manifest
import android.annotation.SuppressLint
//import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.room.Room
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil


class Position : Fragment() {
    // Database variables/values
//    lateinit var database: LocationsDatabase
    private lateinit var referenceLocationRepo: ReferenceLocationRepo

    // Navigation buttons
    private lateinit var  historyButton: TextView
    private lateinit var  graphButton: TextView

    // Variable for position
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: SimpleLocationItem? = null
    var latitudeField: TextView? = null
    var longitudeField: TextView? = null
    var altitudeField: TextView? = null

//    var checkForDelete = true

//    override fun onAttach(context: Context) {
//        Log.d("Execution", "Position fragment attached")
//        database = Room.databaseBuilder(context, LocationsDatabase::class.java, "locations").build()
//        super.onAttach(context)
//    }

    companion object{
        const val OLDEST_DATA = 1000 * 60 * 5                                       // Oldest Position: 5min old (millis)(considering 1 location/s)
        const val REFRESH_TIME = 1000                                               // Read position every 1s
        val MAX_SIZE = ceil((OLDEST_DATA / REFRESH_TIME).toDouble())                // Max queue size
    }

//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        if (context is Activity) {
//            mActivity = context as FragmentActivity?
//        }
//    }
    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        // Database management
//        database = Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations").build()
        referenceLocationRepo = ViewModelProvider(requireActivity()).get(ReferenceLocationRepo::class.java)

        val view = inflater.inflate(R.layout.position_page, container, false)
        var permissionObtained = false
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


        try{
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

            Log.d("LocationService", "Object correctly created")
        }catch (e: IllegalStateException){
            Log.d("LocationServiceError", "RequireContext returned null")
            return view
        }

        latitudeField = view.findViewById(R.id.latitude_field)
        longitudeField = view.findViewById(R.id.longitude_field)
        altitudeField = view.findViewById(R.id.altitude_field)

        if(savedInstanceState != null){
            latitudeField?.text = savedInstanceState.getString("latitude")
            longitudeField?.text = savedInstanceState.getString("longitude")
            altitudeField?.text = savedInstanceState.getString("altitude")
        }

        // Wait until there are some data. If there are too much elements, delete all data older than OLDEST_DATA
        referenceLocationRepo.allLocations.observe(
            requireActivity()
        ) { locations ->
            if (locations != null && locations.size >= MAX_SIZE) {
//                Log.d("Execution", "Current ${locations.size} locations stored: $locations")
//                Log.d("Execution", "First element: ${locations[0].location}")
                Log.d("Execution", "Before deleting old data")
                referenceLocationRepo.deleteOld()
                Log.d("Execution", "Current ${referenceLocationRepo.allLocations.value?.size} data: ${referenceLocationRepo.allLocations.value}")
            }

        }

        Log.d("Execution", "Before locationPermissionRequest")
        val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            run {
                permissionObtained = true
                for (permission in permissions) {
                    Log.d("Permissions", "Checking: $permission")
                    permissionObtained = permissionObtained && permission.value
                }
                Log.d("Permissions", "Checking permissions..")
                  if(permissionObtained){
                      Log.d("Permissions", "Permission obtained")
                      val locationRequest: LocationRequest = LocationRequest.create()
                      locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        locationRequest.interval = REFRESH_TIME.toLong()


                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            object : LocationCallback() {
                                // Called when device location information is available.
                                override fun onLocationResult(locationResult : LocationResult){
                                    val lastLocation = locationResult.lastLocation
                                    currentLocation = SimpleLocationItem(lastLocation.latitude, lastLocation.longitude, lastLocation.altitude/*, lastLocation.time*/)
                                    latitudeField?.text = currentLocation?.latitude.toString()             // Display data if a location is available
                                    longitudeField?.text = currentLocation?.longitude.toString()
                                    altitudeField?.text = currentLocation?.altitude.toString()
                                    Log.d("Data read", "time: ${lastLocation.time} " +
                                            "- lat: ${currentLocation?.latitude.toString()} " +
                                            "- long: ${currentLocation?.longitude.toString()} " +
                                            "- alt: ${currentLocation?.altitude.toString()}")

                                    // If there are too much elements, delete all data
                                    /* referenceLocationRepo.allLocations.observe(
                                        requireActivity()
                                    ) { locations ->
                                        if (locations != null && locations.size >= MAX_SIZE) {
                                            Log.d("Execution", "Current ${locations.size} locations stored: $locations")
                                            Log.d("Execution", "Deleting old data")
                                            referenceLocationRepo.deleteOld()
                                        }

                                    }*/

                                    // To avoid app crashing, check control not null
//                                    getView()?.let{
                                        // Insert an entry with valid currentLocation
                                        activity?.lifecycleScope?.launch {
                                            Log.d("Execution", "CoroutineScope")
                                            // If there are too much elements, delete all data
                                            /*if(referenceLocationRepo.allLocations.value?.size!! >= MAX_SIZE) {
                                                Log.d("Execution", "Deleting old locations")
                                                referenceLocationRepo.deleteOld()
                                            }
                                            if(database.locationDao().getAllLocations().value?.size!! >= MAX_SIZE)
                                                database.locationDao().deleteOld()*/

                                                // Then insert new valid position
                                                val entry = LocationEntity(lastLocation.time, currentLocation)
                                                Log.d("Execution", "Saving new location with timestamp: ${entry.timeStamp} and location: ${entry.location}")
                                                referenceLocationRepo.insert(entry)
//                                                checkForDelete = true
//                                            }
                                        }
//                                    }
                                    super.onLocationResult(locationResult)
                                }
                            },
                            requireContext().mainLooper
                        )
                }
            }
        }
        Log.d("Execution", "After locationPermissionRequest: $locationPermissionRequest")
        if(!permissionObtained) {
            Log.d("Permissions", "Asking permissions")
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }else
            Log.d("Permissions", "Permissions already got")
        return view
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle){
        savedInstanceState.putString("latitude", latitudeField?.text.toString())
        savedInstanceState.putString("longitude", longitudeField?.text.toString())
        savedInstanceState.putString("altitude", altitudeField?.text.toString())
        super.onSaveInstanceState(savedInstanceState)
    }

}















