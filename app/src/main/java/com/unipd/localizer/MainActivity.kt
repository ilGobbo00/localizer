package com.unipd.localizer

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.os.persistableBundleOf
import androidx.room.Room

class MainActivity : AppCompatActivity() {
//    private lateinit var locationDatabase : LocationsDatabase

//    override fun onCreate(savedInstanceState: Bundle?) {
//        if(persistentState == null) {
//            val db = Room.databaseBuilder(
//                applicationContext,
//                LocationsDatabase::class.java,
//                "locations"
//            ).build()
//
//            val persistentState = this.getPreferences(MODE_PRIVATE)
//            val persistentStateEditor = persistentState.edit()
//
//            persistentStateEditor.pu
//        }
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("Application","App starting")

//        var locationDatabase = null // = Room.databaseBuilder(applicationContext, LocationsDatabase::class.java, "recorded-locations").build()
//        if(persistentState == null)
//            locationDatabase = Room.databaseBuilder(applicationContext, LocationsDatabase::class.java, "recorded-locations").build()
//        else
//            locationDatabase = TODO("Mettere l'ottenumento dei dati persistenti")

    }
}