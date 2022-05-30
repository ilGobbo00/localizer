package com.unipd.localizer

import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.os.persistableBundleOf
import androidx.room.Room

class MainActivity : AppCompatActivity() {
    private lateinit var persistentState: SharedPreferences
    private lateinit var persistentStateEditor: SharedPreferences.Editor

    private var permissionObtained = false

    companion object{
        const val PERMISSIONS = "permissions"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Get SharedPreferences reference
        persistentState = this.getPreferences(MODE_PRIVATE)
        persistentStateEditor = persistentState.edit()

        val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissionObtained = true
            for (permission in permissions) {
                Log.i("Localizer/Permissions", "Checking: $permission")
                permissionObtained = permissionObtained && permission.value
            }

            persistentStateEditor.putBoolean(PERMISSIONS, permissionObtained)
            persistentStateEditor.apply()
        }

        if(!permissionObtained) {
            Log.i("Localizer/Permissions", "Asking permissions")
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                locationPermissionRequest.launch(arrayOf(Manifest.permission.FOREGROUND_SERVICE))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }else
            Log.i("Localizer/Permissions", "Permissions already got")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
}