package com.unipd.localizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import java.sql.Timestamp

class LocationAdapter(private val locationList: List<LocationEntity>) :
RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(){

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationLabel: TextView = itemView.findViewById(R.id.location_label)

        fun bind(word: String) {
            locationLabel.text = word
        }
    }
    private val onClickListener = View.OnClickListener { view ->
        val locationDateTime = view.findViewById<TextView>(R.id.location_label).text.toString()    // Get human readable time
        val seeDetails = HistoryDirections.actionHistoryPageToDetailPage(locationDateTime)          // TODO Trovare il metodo di passare un oggetto di tipo LocationEntity
        Navigation.findNavController(view).navigate(seeDetails)
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
            locationStringList.add(Timestamp(location.timeStamp).toString())      // TODO provare dd-mm-yyyy hh-mm-ss
//        val locationList = locationDao.getAllLocationsStringTimestamp()         // TODO Probabilmente crasha
        holder.bind(locationStringList[position])
    }
}