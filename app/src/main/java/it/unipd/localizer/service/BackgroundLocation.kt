package it.unipd.localizer.service

import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
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
class BackgroundLocation : Service() {
    //region Flag for permissions and service status
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
        // Called when device location information is available.
        override fun onLocationResult(locationResult : LocationResult){
            val lastLocation = locationResult.lastLocation
            currentLocation = SimpleLocationItem(lastLocation.latitude, lastLocation.longitude, lastLocation.altitude)

            // Then insert new valid position
            val entry = LocationEntity(lastLocation.time, currentLocation)
            Log.i("Localizer/Background", "Saving - " +
                    "${lastLocation.time} | " +
                    "${currentLocation.latitude} | " +
                    "${currentLocation.longitude}  | " +
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

    companion object {
        //region Persistent state keys
        const val BACKGROUND_SERVICE = "background_service"
        const val CHANNEL_ID = "localizer_channel"
        //endregion
    }

    override fun onCreate() {
        Log.d("Localizer/Background", "onCreate")

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
        Log.d("Localizer/Background", "onStartCommand")
        permissionsObtained = intent?.getBooleanExtra(PERMISSIONS, false) ?: false
        serviceActiveNewRequest = intent?.getBooleanExtra(BACKGROUND_SERVICE, false) ?: false
        if(serviceActiveNewRequest && permissionsObtained) {
            backgroundLocalizer()
        }
        return START_REDELIVER_INTENT
    }

    @SuppressLint("MissingPermission") // If here permissions are already obtained
    private fun backgroundLocalizer(){
        Log.d("Localizer/Background", "Permissions obtained, start backgroundLocalizer")

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
        Log.d("Localizer/Background", "onDestroy")
        if(this::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        super.onDestroy()
    }
}