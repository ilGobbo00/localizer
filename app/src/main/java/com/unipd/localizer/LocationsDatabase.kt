package com.unipd.localizer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope

// LocationDatabase defines the database configuration and serves as the app's main access point to the persisted data
@Database(entities = [LocationEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocationsDatabase : RoomDatabase(){
    abstract fun locationDao(): LocationDao

    companion object{
        @Volatile
        private var INSTANCE: LocationsDatabase? = null

        fun getDatabase(context : Context, scope: CoroutineScope): LocationsDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationsDatabase::class.java,
                "locations")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

//class insertAsyncLocation internal constructor(private val locationDao: LocationDao): AsyncTask<Location, Void, Void>(){
//
//    override fun doInBackground(vararg l: Location?): Void? {
//            l[0]?.let { locationDao.insert(it) }
//        return null
//    }
//}