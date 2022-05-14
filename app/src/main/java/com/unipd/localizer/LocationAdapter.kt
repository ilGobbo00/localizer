package com.unipd.localizer

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationAdapter(private val positionsList: List<Location>, private val database: LocationsDatabase) :
RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(){

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationLabel: TextView = itemView.findViewById(R.id.location_label)

        fun bind(word: String) {
            locationLabel.text = word
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        val location = view.findViewById<TextView>(R.id.location_label).text.toString()
        // TODO - Da implementare la navigazione (crezione di altro xml e classe fragment)
//        val action = HistoryDirections.actionListFragmentToDetailFragment(location)
//        Navigation.findNavController(view).navigate(action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false)
        view.setOnClickListener(onClickListener)
        return LocationViewHolder(view)
    }

    // Returns size of data list
    override fun getItemCount(): Int {
        return positionsList.size
    }

    // Displays data at a certain position
    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(database.locationDao().getAllLocationsStringTimestamp()[position])
    }
}