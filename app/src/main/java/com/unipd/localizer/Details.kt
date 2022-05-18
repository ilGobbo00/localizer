package com.unipd.localizer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.room.Database
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.text.SimpleDateFormat

class Details:Fragment() {
    lateinit var backButton: FloatingActionButton
    private lateinit var referenceLocationRepo: ReferenceLocationRepo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("Execution", "Start of Detail")

        referenceLocationRepo = ViewModelProvider(requireActivity()).get(ReferenceLocationRepo::class.java)

        val view = inflater.inflate(R.layout.location_detail, container, false)
        if(view == null)
            return view
        backButton = view.findViewById(R.id.back_to_list)
        backButton.setOnClickListener { clickView ->
            val destinationTab = DetailsDirections.actionDetailPageToHistoryPage()
            Navigation.findNavController(clickView).navigate(destinationTab)
        }


        // TODO Debug
        val test = view.findViewById<TextView>(R.id.time_label)
        val timestampString /*test.text*/ = DetailsArgs.fromBundle(requireArguments()).location
        val timestamp = SimpleDateFormat("dd-MM-yyyy kk:mm:ss.SSS").parse(timestampString).time

        runBlocking(Dispatchers.IO){
            Log.d("CoExecution", "Location got from db: ${referenceLocationRepo.getLocation(timestamp)}")
        }
//        referenceLocationRepo.lastRequestedLocation?.observe(viewLifecycleOwner){ locationEntity ->
//            Log.d("Execution", "Location passed to fragment: ${ locationEntity.toString()}")
//
//        }
//        val db = Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations").build()
//        suspend {
//            test.text = db.locationDao().getLocation(SimpleDateFormat("dd-MM-yyyy kk:mm:ss.SSS").parse(timestampString).time).toString()
//        }
//        referenceLocationRepo.getLocation(SimpleDateFormat("dd-MM-yyyy kk:mm:ss.SSS").parse(timestampString).time)
//        Log.d("Execution", "Location searced with timestamp: ${SimpleDateFormat("dd-MM-yyyy kk:mm:ss.SSS").parse(timestampString).time}")
//        test.text = referenceLocationRepo.lastRequestedLocation?.value.toString()
//        referenceLocationRepo.lastRequestedLocation?.observe(requireActivity()){
//            locationEntity ->
//            test.text = locationEntity?.toString()
//            Log.d("Execution", "Location detail. ${test.text}")
//        }

//        viewLifecycleOwner.lifecycleScope.launch {
//            referenceLocationRepo.getLocation(SimpleDateFormat("dd-MM-yyyy kk:mm:ss.SSS").parse(timestampString).time)
//        }

            Log.d("Execution", "Return from Detail")
            return view
        }

}
//}