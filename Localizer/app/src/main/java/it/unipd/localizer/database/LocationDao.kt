package it.unipd.localizer.database

import androidx.room.*
import it.unipd.localizer.tabs.Position.Companion.OLDEST_DATA
import java.util.*

// LocationDao provides the methods that the rest of the app uses to interact with data in the locations table
@Dao
interface LocationDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertLocation(location: LocationEntity)

        @Query("SELECT * FROM locations ORDER BY timeStamp DESC")
        suspend fun getAllLocations(): List<LocationEntity>

        @Query("SELECT * FROM locations WHERE timeStamp = :timestamp")
        suspend fun getLocation(timestamp: Long): LocationEntity?

        @Query("DELETE FROM locations")
        suspend fun deleteAll()

        @Transaction
        suspend fun deleteOld(){
                deleteOld(Date().time, OLDEST_DATA.toLong())
        }

        // Deleting data older than threshold
        @Query("DELETE FROM locations WHERE :currentTime - timeStamp > :threshold")
        suspend fun deleteOld(currentTime: Long, threshold: Long)

}