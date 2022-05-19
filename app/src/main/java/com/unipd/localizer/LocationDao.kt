package com.unipd.localizer

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.*
import com.unipd.localizer.Position.Companion.OLDEST_DATA
import java.sql.Timestamp
import java.util.*

// LocationDao provides the methods that the rest of the app uses to interact with data in the locations table
@Dao
interface LocationDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertLocation(location: LocationEntity)

        @Query("SELECT * FROM locations ORDER BY timeStamp DESC")
        fun getAllLocations(): LiveData<List<LocationEntity>>

//        @Query("SELECT * FROM locations WHERE timeStamp = :timestamp")
//        fun getLocation(timestamp: Long): LiveData<LocationEntity>

        @Query("SELECT * FROM locations WHERE timeStamp = :timestamp")
        fun getLocation(timestamp: Long): LocationEntity?

        @Query("SELECT MIN(timeStamp) FROM locations")
        fun getOldestTimeSaved() : Long?

        @Transaction
        fun deleteOld(){
//                deleteOld(SystemClock.elapsedRealtimeNanos(), (OLDEST_DATA * 1000).toLong())
                deleteOld(Date().time, OLDEST_DATA.toLong())
        }

        @Query("DELETE FROM locations")
        fun deleteAll()

        // Deleting data older than 5 minutes
        @Query("DELETE FROM locations WHERE :currentTime - timeStamp > :threshold")
        fun deleteOld(currentTime: Long, threshold: Long)

//        @Transaction
//        fun getAllLocationsStringTimestamp(): List<String>{
//                val locationList = getAllLocations()
//                val strList = mutableListOf<String>()
//
//                // TODO - Sicuramente crasha quando cerca di fare locationList.value, Da provare
//                for(location in locationList.value!!)
//                        strList.add(location.timeStamp.toString())    // Only a no null location is stored
//
//                return strList
//        }

}