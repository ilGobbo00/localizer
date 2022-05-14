package com.unipd.localizer

import android.location.Location
import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.ByteArrayOutputStream

class Converters {
    @TypeConverter
    fun fromLocation(location: Location?): String?{
        return Gson().toJson(location)
    }

    @TypeConverter
    fun toLocation(string: String?): Location?{
        return try{
            Gson().fromJson<Location>(string, Location::class.java)
        }catch (e: JsonSyntaxException){
            Log.d("Execution", "Failed to convert json -> Location $string")
            null
        }
    }
}