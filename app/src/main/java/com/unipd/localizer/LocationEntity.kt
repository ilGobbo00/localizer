package com.unipd.localizer

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

// Each instance of LocationRead represents a row in a locations table in the app's database
@Entity (tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = false) val timeStamp: Long,
    val location: SimpleLocationItem
)