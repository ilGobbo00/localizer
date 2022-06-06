package it.unipd.localizer.database

import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class Converters {
    @TypeConverter
    fun fromLocation(location: SimpleLocationItem?): String?{
//        Log.d("Room", "Location before conversion: $location")
//        val storable = Gson().toJson(location)
//        Log.d("Room", "Location after conversion: $storable")
        return Gson().toJson(location)
    }

    @TypeConverter
    fun toLocation(string: String?): SimpleLocationItem?{
        return try{
            Gson().fromJson(string, SimpleLocationItem::class.java)
        }catch (e: JsonSyntaxException){
            Log.d("Execution", "Failed to convert json -> LocationItem $string")
            null
        }
    }
}