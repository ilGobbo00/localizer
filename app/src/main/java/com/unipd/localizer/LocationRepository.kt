package com.unipd.localizer

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class LocationRepository(private val locationDao: LocationDao) {
    val allLocations: LiveData<List<LocationEntity>> = locationDao.getAllLocations()
//    val allLocationsStringTimestamp: List<String> = locationDao.getAllLocationsStringTimestamp()

    @WorkerThread
    suspend fun insert(location: LocationEntity){
        locationDao.insertLocation(location)
    }

//    @WorkerThread
//    suspend fun getLocation(timestamp: Long): LiveData<LocationEntity> {
//        return locationDao.getLocation(timestamp)
//    }

    @WorkerThread
    suspend fun getAllLocations(): List<LocationEntity>? {
        return locationDao.getAllLocations().value          // TODO Vedere se si spacca
    }

    @WorkerThread
    suspend fun getLocation(timestamp: Long): LocationEntity? {
        return locationDao.getLocation(timestamp)
    }

    @WorkerThread
    suspend fun getOldestLocation(): Long? {
        return locationDao.getOldestTimeSaved()
    }

    @WorkerThread
    suspend fun deleteAll(){
        locationDao.deleteAll()
    }

    @WorkerThread
    suspend fun deleteOld(){
        locationDao.deleteOld()
    }

}