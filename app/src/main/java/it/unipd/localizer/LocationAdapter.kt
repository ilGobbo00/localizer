package it.unipd.localizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import it.unipd.localizer.database.LocationEntity
import it.unipd.localizer.tabs.HistoryDirections
import java.text.SimpleDateFormat
import java.util.*

class LocationAdapter(private val locationList: List<LocationEntity>) :
RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(){

    companion object{
        const val TIMESTAMP_PATTERN = "dd/MM/yyyy kk:mm:ss.SSS"
        const val DAY_PATTERN = "dd/MM/yyyy"
        const val HOUR_PATTERN = "kk:mm:ss.SSS"
    }

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationLabel: TextView = itemView.findViewById(R.id.location_label)
        private val locationNum: TextView = itemView.findViewById(R.id.location_num)

        fun bind(position: Int, word: String) {
            locationNum.text = position.plus(1).toString()
            locationLabel.text = word
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        val locationDateTime = view.findViewById<TextView>(R.id.location_label).text.toString()    // Get human readable time
        val seeDetails = HistoryDirections.actionHistoryPageToDetailPage(locationDateTime)
        Navigation.findNavController(view).navigate(seeDetails)

        // Get SharedPreferences reference
//        persistentState = activity?.getPreferences(Context.MODE_PRIVATE)
//        persistentStateEditor = persistentState?.edit()
//        persistentStateEditor?.putBoolean(SHOW_DETAILS, true)
//        persistentStateEditor?.apply()
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
            locationStringList.add(SimpleDateFormat(TIMESTAMP_PATTERN, Locale.ITALY).format(location.timeStamp))
        holder.bind(position, locationStringList[position])
    }
}