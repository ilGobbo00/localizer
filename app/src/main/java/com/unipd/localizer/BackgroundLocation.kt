package com.unipd.localizer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.room.Room
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BackgroundLocation : Service() {
    private var serviceActiveNewRequest = false
    private var serviceActiveOldRequest = false
    private var permissionsObtained = false

    private val locationCallback = object : LocationCallback() {
        // Called when device location information is available.
        override fun onLocationResult(locationResult : LocationResult){
            val lastLocation = locationResult.lastLocation
            currentLocation = SimpleLocationItem(lastLocation.latitude, lastLocation.longitude, lastLocation.altitude)

            // Then insert new valid position
            val entry = LocationEntity(lastLocation.time, currentLocation)
            Log.i("Localizer/Background", "Saving ? - " +
                    "${lastLocation.time} | " +
                    "${currentLocation?.latitude.toString()} | " +
                    "${currentLocation?.longitude.toString()}  | " +
                    currentLocation?.altitude.toString())

            runBlocking{
                launch{
                    dbManager.insertLocation(entry)
                }
//                locationsDatabase.locationDao().insertLocation(entry) // TODO Cercare di fare con ReferenceLocationRepo
            }
//                    }
            super.onLocationResult(locationResult)
        }
    }


    // Database variables/values
//    private lateinit var referenceLocationRepo: ReferenceLocationRepo
    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao

    // Variable for position
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: SimpleLocationItem? = null


    companion object {
        const val BACKGROUND_SERVICE = "background_service"
        const val CHANNEL_ID = "localizer_channel"
    }

    override fun onCreate() {
        Log.d("Localizer/Service", "onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
        super.onCreate()
    }


    // Only Localizer can stop and start background service
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Localizer/Background", "onStartCommand")
        serviceActiveNewRequest = intent?.getBooleanExtra(BACKGROUND_SERVICE, false) ?: false
        Log.d("Localizer/Background", "oldRequest: $serviceActiveOldRequest, newRequest: $serviceActiveNewRequest")
        if(!serviceActiveOldRequest && serviceActiveNewRequest) {
            serviceActiveOldRequest = serviceActiveNewRequest
            backgroundLocalizer()
        }
        return START_REDELIVER_INTENT
    }

    private fun backgroundLocalizer(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE ) != PackageManager.PERMISSION_GRANTED ) {
            return
        }
        Log.d("Localizer/Service", "Permissions obtained, start backgroundLocalizer")

//        referenceLocationRepo = ViewModelProvider(requireActivity()).get(ReferenceLocationRepo::class.java)
//        val locationsDatabase = LocationsDatabase.getDatabase(this)
        database = Room.databaseBuilder(applicationContext, LocationsDatabase::class.java, "locations").build()
        dbManager = database.locationDao()

        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = Position.REFRESH_TIME.toLong()

//        try{
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
//        }catch (e: IllegalStateException){
//            Log.d("LocationServiceError", "RequireContext returned null")
//            return view
//        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        val notificationBuilder: Notification.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = Notification.Builder(applicationContext, CHANNEL_ID)
            notificationBuilder.setContentTitle(getString(R.string.app_name))
            notificationBuilder.setContentText(getString(R.string.app_description))
            notificationBuilder.setSmallIcon(R.drawable.temp_notification_icon)
            val notification = notificationBuilder.build()
            val notificationID = 1224272
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                startForeground(notificationID, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
            else
                startForeground(notificationID, notification)
        }
    }

    override fun onDestroy() {
        Log.d("Localizer/Service", "onDestroy")
        stopForeground(true)
        serviceActiveOldRequest = false
        super.onDestroy()
    }
}