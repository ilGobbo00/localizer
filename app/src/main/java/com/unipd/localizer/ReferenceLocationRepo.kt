package com.unipd.localizer

import android.app.Application
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReferenceLocationRepo(application: Application): AndroidViewModel(application){
    private val repo: LocationRepository

    val allLocations: LiveData<List<LocationEntity>>
//    var lastRequestedLocation: LiveData<LocationEntity>? = null
    lateinit var lastRequestedLocation: LocationEntity

    init{
        val locationsDao = LocationsDatabase.getDatabase(application/*, viewModelScope*/).locationDao()
        repo = LocationRepository(locationsDao)
        allLocations = repo.allLocations
    }

    fun insert(location: LocationEntity) = viewModelScope.launch(Dispatchers.IO){
        repo.insert(location)
    }

//    fun getLocation(timestamp: Long) = viewModelScope.launch(Dispatchers.Default){
//        Log.d("CoExecution", "Time to find: $timestamp")
//        lastRequestedLocation = repo.getLocation(timestamp)
//    }

    suspend fun getAllLocations(): List<LocationEntity>? {
        return repo.getAllLocations()
    }

    suspend fun getLocation(timestamp: Long): LocationEntity? {
//        Log.d("CoExecution", "Time to find: $timestamp")
        return repo.getLocation(timestamp)
    }

    suspend fun getOldestLocation(): Long?{
        return repo.getOldestLocation()
    }

    fun deleteAll()= viewModelScope.launch(Dispatchers.IO){
        repo.deleteAll()
    }

    fun deleteOld() = viewModelScope.launch(Dispatchers.IO){
        repo.deleteOld()
    }
}