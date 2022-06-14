package it.unipd.localizer.database

import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

// Class to convert SimpleLocationItem for the database management
class Converters {
    @TypeConverter
    fun fromLocation(location: SimpleLocationItem?): String?{
        return Gson().toJson(location)
    }

    @TypeConverter
    fun toLocation(string: String?): SimpleLocationItem?{
        return try{
            Gson().fromJson(string, SimpleLocationItem::class.java)
        }catch (e: JsonSyntaxException){
            Log.d("Localizer/C", "Failed to convert json -> SimpleLocationItem $string")
            null
        }
    }
}