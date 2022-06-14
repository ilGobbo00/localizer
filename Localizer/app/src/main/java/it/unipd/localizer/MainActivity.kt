package it.unipd.localizer

import android.Manifest.permission.*
import android.content.SharedPreferences
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import it.unipd.localizer.tabs.Details.Companion.BACK_PRESSED

class MainActivity : AppCompatActivity() {
    //region Peristent state varables
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor
    //endregion

    // Flag for permissions management
    private var permissionObtained = false

    companion object{
        // Constants for persistentState
        const val PERMISSIONS = "permissions"
        const val SERVICE_RUNNING = "main_service"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        persistentState = this.getPreferences(MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

        // Checking permissions
        val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionObtained = ContextCompat.checkSelfPermission(applicationContext, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(applicationContext, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

            persistentStateEditor.putBoolean(PERMISSIONS, permissionObtained)
            persistentStateEditor.apply()

            // Set service to false, used in Position fragment
            if(!permissionObtained)
                persistentStateEditor.putBoolean(SERVICE_RUNNING, false)
        }

        // Asking permission if not granted
        if(!permissionObtained) {
            Log.i("Localizer/MA", "Asking permissions")
            locationPermissionRequest.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                locationPermissionRequest.launch(arrayOf(FOREGROUND_SERVICE))
        }

        super.onStart()
    }

    // Management of back button pressure in Details
    override fun onBackPressed() {
        persistentStateEditor.putBoolean(BACK_PRESSED, true)
        persistentStateEditor.apply()
        super.onBackPressed()
    }
}