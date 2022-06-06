package com.unipd.localizer

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.unipd.localizer.Details.Companion.SHOW_DETAILS
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.sql.Timestamp
import java.text.SimpleDateFormat
import kotlin.coroutines.coroutineContext

class LocationAdapter(private val locationList: List<LocationEntity>, private val activity: FragmentActivity?) :
RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(){

    // Shared preferences for start/stop background service button
    private var persistentState: SharedPreferences? = null
    private var persistentStateEditor: SharedPreferences.Editor? = null

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationLabel: TextView = itemView.findViewById(R.id.location_label)

        fun bind(word: String) {
            locationLabel.text = word
        }

    }
    private val onClickListener = View.OnClickListener { view ->
        val locationDateTime = view.findViewById<TextView>(R.id.location_label).text.toString()    // Get human readable time
        val seeDetails = HistoryDirections.actionHistoryPageToDetailPage(locationDateTime)
        Navigation.findNavController(view).navigate(seeDetails)

        // Get SharedPreferences reference
        persistentState = activity?.getPreferences(Context.MODE_PRIVATE)
        persistentStateEditor = persistentState?.edit()
        persistentStateEditor?.putBoolean(SHOW_DETAILS, true)
        persistentStateEditor?.apply()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false)
        view.setOnClickListener(onClickListener)
        return LocationViewHolder(view)
    }

    // Returns size of data list
    override fun getItemCount(): Int {
        return locationList.size
    }

    // Displays data at a certain position
    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val locationStringList = mutableListOf<String>()
        for(location in locationList)
            locationStringList.add(SimpleDateFormat("dd-MM-yyyy kk:mm:ss.SSS").format(location.timeStamp))
        holder.bind(locationStringList[position])
    }
}