package com.unipd.localizer

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat

class Details:Fragment() {
    var backButton: FloatingActionButton? = null
    private lateinit var referenceLocationRepo: ReferenceLocationRepo

    // Labels
    private var time: TextView? = null
    private var latitude: TextView? = null
    private var longitude: TextView? = null
    private var altitude: TextView? = null


    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        referenceLocationRepo = ViewModelProvider(requireActivity()).get(ReferenceLocationRepo::class.java)

        val view = inflater.inflate(R.layout.location_detail, container, false)
        backButton = view?.findViewById(R.id.back_to_list)          // Due to orientation change, view can be null
        backButton?.setOnClickListener { clickView ->
            val destinationTab = DetailsDirections.actionDetailPageToHistoryPage()
            Navigation.findNavController(clickView).navigate(destinationTab)
        }

        // Reference to labels
        time = view?.findViewById(R.id.location_time_detail)
        latitude = view?.findViewById(R.id.location_latitude_detail)
        longitude = view?.findViewById(R.id.location_longitude_detail)
        altitude = view?.findViewById(R.id.location_altitude_detail)

        // Get parameter passed thought fragment.
        // Use the timestamp (unique) to obtain the element from DB after conversion from human-readable date to epoch
        val epochString: String = DetailsArgs.fromBundle(requireArguments()).location
        val timestamp: Long? = SimpleDateFormat("dd-MM-yyyy kk:mm:ss.SSS").parse(epochString)?.time


        // Location to display
        var locationToDisplay: LocationEntity?
        runBlocking(Dispatchers.IO){
            locationToDisplay = referenceLocationRepo.getLocation(timestamp!!)
//            Log.d("CoExecution", "Location got from db: ${referenceLocationRepo.getLocation(timestamp)}")
        }

        if(locationToDisplay == null){
            val errorDay: String = SimpleDateFormat("dd-MM-yyyy").format(timestamp)
            val errorTime: String = SimpleDateFormat("kk:mm:ss.SSS").format(timestamp)
            time?.text = getString(R.string.invalid_location_detail, errorDay, errorTime)
            latitude?.text = getString(R.string.invalid_home_data)
            longitude?.text = getString(R.string.invalid_home_data)
            altitude?.text = getString(R.string.invalid_home_data)
            return view
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            time?.text = Html.fromHtml(getString(R.string.time_location_detail, SimpleDateFormat("dd-MM-yyy").format(timestamp), SimpleDateFormat("kk:mm:ss.SSS").format(timestamp)), FROM_HTML_MODE_LEGACY)
            latitude?.text = Html.fromHtml(getString(R.string.latitude_read_detail, locationToDisplay!!.location!!.latitude.toString()), FROM_HTML_MODE_LEGACY)
            longitude?.text = Html.fromHtml(getString(R.string.longitude_read_detail, locationToDisplay!!.location!!.longitude.toString()), FROM_HTML_MODE_LEGACY)
            altitude?.text = Html.fromHtml(getString(R.string.altitude_read_detail, locationToDisplay!!.location!!.altitude.toString()), FROM_HTML_MODE_LEGACY)
        }else{
            time?.text = getString(R.string.time_location_detail, SimpleDateFormat("dd-MM-yyy").format(timestamp), SimpleDateFormat("kk:mm:ss.SSS").format(timestamp))
            latitude?.text = getString(R.string.latitude_read_detail, locationToDisplay!!.location!!.latitude.toString())
            longitude?.text = getString(R.string.longitude_read_detail, locationToDisplay!!.location!!.longitude.toString())
            altitude?.text = getString(R.string.altitude_read_detail, locationToDisplay!!.location!!.altitude.toString())
        }

        Log.d("Execution", "Return from Detail")
        return view
    }
}
//}