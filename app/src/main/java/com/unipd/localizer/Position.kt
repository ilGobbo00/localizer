package com.unipd.localizer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.gms.location.*


class Position : Fragment() {

    private lateinit var  historyButton: TextView
    private lateinit var  graphButton: TextView

    // Variable for position
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    companion object{
        const val OLDEST_DATA = 1000 * 60 * 5                                       // Oldest Position 5min old
        const val REFRESH_TIME = 1000                                               // Read position every 1s
        val MAX_LIST_SIZE =
            Math.ceil((OLDEST_DATA / REFRESH_TIME).toDouble())           // Max queue size
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.position_page, container, false)
        var permissionObtained: Boolean = false
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

        val latitudeField: TextView = view.findViewById(R.id.latitude_field)
        val longitudeField: TextView = view.findViewById(R.id.longitude_field)

        Log.d("Runtime", "Before locationPermissionRequest")
        val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            run {
//                Log.d("Check permissions", "checking..")
//                var tempPerms = true
                permissionObtained = true
                for (permission in permissions) {
                    Log.d("Check permission", permission.toString())
                    permissionObtained = permissionObtained && permission.value
                }
//
//
                Log.d("Permissions", "Checking permissions..")
//                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    Log.d("Permissions", "Missing permissions. Require permission to user")
//                    ActivityCompat.requestPermissions(requireContext() as Activity,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
//                }else{
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
                                    currentLocation = locationResult.lastLocation

                                    latitudeField.text = currentLocation?.latitude.toString()             // Display data if a location is available
                                    longitudeField.text = currentLocation?.longitude.toString()
                                    Log.d("Data read", "lat: ${currentLocation?.latitude.toString()} - long: ${currentLocation?.longitude.toString()}")

                                    super.onLocationResult(locationResult)
                                }
                            },
                            requireContext().mainLooper
                        )
                }
            }
        }
        Log.d("Runtime", "After locationPermissionRequest: $locationPermissionRequest")
        if(!permissionObtained) {
            Log.d("Permissions", "Asking permissions")
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }else
            Log.d("Permissions", "Permissions already got")

        return view
    }

}















