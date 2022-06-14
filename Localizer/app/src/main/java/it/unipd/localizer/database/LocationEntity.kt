package it.unipd.localizer.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Each instance of read location represents a row in a locations table in the app's database
@Entity (tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = false) val timeStamp: Long,      // timestamp saved once and used as primary key
    val location: SimpleLocationItem                            // Contains laatitude, longitude, altitude
)