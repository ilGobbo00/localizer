package it.unipd.localizer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import it.unipd.localizer.MainActivity.Companion.PERMISSIONS
import it.unipd.localizer.R
import it.unipd.localizer.database.LocationDao
import it.unipd.localizer.database.LocationEntity
import it.unipd.localizer.database.LocationsDatabase
import it.unipd.localizer.database.SimpleLocationItem
import it.unipd.localizer.tabs.Position
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Class for foreground service
class ForegroundLocation : Service() {
    //region Flags for permissions and service status
    private var serviceActiveNewRequest = false
    private var permissionsObtained = false
    //endregion

    //region Database variables
    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao
    //endregion

    //region Variables for position
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: SimpleLocationItem
    //endregion

    // Callback for requestLocationUpdates
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult : LocationResult){
            val lastLocation = locationResult.lastLocation ?: return
            currentLocation = SimpleLocationItem(lastLocation.latitude, lastLocation.longitude, lastLocation.altitude)

            // Then insert new valid position
            val entry = LocationEntity(lastLocation.time, currentLocation)

            runBlocking{
                launch{
                    Log.i("Localizer/F", "Saving: " +
                            "${lastLocation.time} | " +
                            "${currentLocation.latitude} | " +
                            "${currentLocation.longitude}  | " +
                            currentLocation.altitude.toString())
                    dbManager.insertLocation(entry)
                }
                launch {
                    // Check for data to delete
                    val locationList = dbManager.getAllLocations()
                    if (locationList.size > Position.MAX_SIZE) {
                        Log.i("Localizer/F", "Deleting oldest data..")
                        dbManager.deleteOld()
                    }
                }
            }
            super.onLocationResult(locationResult)
        }
    }

    companion object {
        //region Persistent state keys
        const val FOREGROUND_SERVICE = "background_service"
        const val CHANNEL_ID = "localizer_channel"
        //endregion
    }

    override fun onCreate() {
        // Notification creation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        super.onCreate()
    }

    // Only Localizer can stop and start background service
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        permissionsObtained = intent?.getBooleanExtra(PERMISSIONS, false) ?: false
        serviceActiveNewRequest = intent?.getBooleanExtra(FOREGROUND_SERVICE, false) ?: false

        // If it was requested to start foregorund service and app has permissions, start foreground service
        return if(serviceActiveNewRequest && permissionsObtained) {
            foregroundLocalizer()
            START_REDELIVER_INTENT
        }else
            START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // If here, permissions are already obtained
    private fun foregroundLocalizer(){
        Log.i("Localizer/F", "Start foreground service")

        //region Database reference
        try {
            database = LocationsDatabase.getDatabase(applicationContext)
        }catch (e: java.lang.IllegalStateException){
            Log.e("Localizer/F", "Can't create database due to context error")
            return
        }
        dbManager = database.locationDao()
        //endregion

        //region Create a location request
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = PRIORITY_HIGH_ACCURACY
        locationRequest.interval = Position.REFRESH_TIME.toLong()
        //endregion

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        val notificationBuilder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(applicationContext, CHANNEL_ID)
            else
                Notification.Builder(applicationContext)

        notificationBuilder.setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.app_description))
            .setSmallIcon(R.drawable.notification_icon)

        val notification = notificationBuilder.build()
        val notificationID = 1224272
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(notificationID, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
        else
            startForeground(notificationID, notification)

    }

    override fun onDestroy() {
        Log.d("Localizer/F", "Stop foreground service")
        if(this::fusedLocationClient.isInitialized)                         // Check possible errors
            fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        super.onDestroy()
    }
}