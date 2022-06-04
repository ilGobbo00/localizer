package com.unipd.localizer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
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
import com.unipd.localizer.MainActivity.Companion.PERMISSIONS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BackgroundLocation : Service() {
    private var serviceActiveNewRequest = false
//    private var serviceActiveOldRequest = false
    private var permissionsObtained = false

    private val locationCallback = object : LocationCallback() {
        // Called when device location information is available.
        override fun onLocationResult(locationResult : LocationResult){
            val lastLocation = locationResult.lastLocation
            currentLocation = SimpleLocationItem(lastLocation.latitude, lastLocation.longitude, lastLocation.altitude)

            // Then insert new valid position
            val entry = LocationEntity(lastLocation.time, currentLocation)
            Log.i("Localizer/Background", "Saving - " +
                    "${lastLocation.time} | " +
                    "${currentLocation.latitude.toString()} | " +
                    "${currentLocation.longitude.toString()}  | " +
                    currentLocation.altitude.toString())

            runBlocking{
                launch{
                    dbManager.insertLocation(entry)
                }
                launch {
                    val locationList = dbManager.getAllLocations()
                    if (locationList.size > Position.MAX_SIZE)
                        dbManager.deleteOld()
                }
            }
            super.onLocationResult(locationResult)
        }
    }


    // Database variables/values
//    private lateinit var referenceLocationRepo: ReferenceLocationRepo
    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao

    // Variable for position
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: SimpleLocationItem

//    private lateinit var persistentState: SharedPreferences
//    private lateinit var persistentStateEditor: SharedPreferences.Editor

    companion object {
        const val BACKGROUND_SERVICE = "background_service"
        const val CHANNEL_ID = "localizer_channel"
    }

    override fun onCreate() {
        Log.d("Localizer/Background", "onCreate")

        // Get SharedPreferences reference
//        persistentState = application.getp .getPreferences(MODE_PRIVATE)
//        persistentStateEditor = persistentState.edit()

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
        permissionsObtained = intent?.getBooleanExtra(PERMISSIONS, false) ?: false
        serviceActiveNewRequest = intent?.getBooleanExtra(BACKGROUND_SERVICE, false) ?: false
//        Log.d("Localizer/Background", "oldRequest: $serviceActiveOldRequest, newRequest: $serviceActiveNewRequest")
        // TODO da controllare la logica
        if(/*!serviceActiveOldRequest &&*/ serviceActiveNewRequest && permissionsObtained) {
//            serviceActiveOldRequest = serviceActiveNewRequest
            backgroundLocalizer()
        }
        return START_REDELIVER_INTENT
    }

    @SuppressLint("MissingPermission") // If here permissions are already obtained
    private fun backgroundLocalizer(){
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE ) != PackageManager.PERMISSION_GRANTED ) {
//            return
//        }
        Log.d("Localizer/Background", "Permissions obtained, start backgroundLocalizer")

//        database = Room.databaseBuilder(applicationContext, LocationsDatabase::class.java, "locations").build()
        try {
            database = LocationsDatabase.getDatabase(applicationContext)
        }catch (e: java.lang.IllegalStateException){
            Log.e("Localizer/B", "Can't create database due to context error")
            return
        }
        dbManager = database.locationDao()

        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = Position.REFRESH_TIME.toLong()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

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
        Log.d("Localizer/Background", "onDestroy")
        if(this::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
//        serviceActiveOldRequest = false
        super.onDestroy()
    }
}