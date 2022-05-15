package com.unipd.localizer

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

// Each instance of LocationRead represents a row in a locations table in the app's database
@Entity (tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = false) val timeStamp: Long,
    val location: SimpleLocationItem?
)
//{
//    @Ignore
//    val debug: String = ""
//}
//{
//    @JvmName("getTimeStamp1")
//    @Ignore
//    fun getTimeStamp(): Long {return timeStamp}
////    fun setTimeStamp(time: Long){timeStamp = time}
//    @JvmName("getLocation1")
//    @Ignore
//    fun getLocation(): Location? {return location}
//}
