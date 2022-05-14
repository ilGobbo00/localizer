package com.unipd.localizer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReferenceLocationRepo(application: Application): AndroidViewModel(application){
    private val repo: LocationRepository

    val allLocations: LiveData<List<LocationEntity>>

    init{
        val locationsDao = LocationsDatabase.getDatabase(application, viewModelScope).locationDao()
        repo = LocationRepository(locationsDao)
        allLocations = repo.allLocations
    }

    fun insert(location: LocationEntity) = viewModelScope.launch(Dispatchers.IO){
        repo.insert(location)
    }

    fun deleteOld() = viewModelScope.launch(Dispatchers.IO){
        repo.deleteOld()
    }
}