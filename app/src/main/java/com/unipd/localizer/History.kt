package com.unipd.localizer

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
//import android.widget.Toast
//import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class History : Fragment(){
    private lateinit var  positionButton: TextView
    private lateinit var  graphButton: TextView
    private lateinit var  deleteButton: FloatingActionButton

//    private lateinit var referenceLocationRepo: ReferenceLocationRepo
    private lateinit var database : LocationsDatabase
    private lateinit var dbManager : LocationDao

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        referenceLocationRepo = ViewModelProvider(requireActivity()).get(ReferenceLocationRepo::class.java)
        database = Room.databaseBuilder(requireContext(), LocationsDatabase::class.java, "locations").build()
        dbManager = database.locationDao()

        //return super.onCreateView(inflater, container, savedInstanceState)
        //super.onCreate(savedInstanceState)
        //view.setContentView(R.layout.position_page)
        val view = inflater.inflate(R.layout.history_page, container, false)

        positionButton = view.findViewById(R.id.position_button)
        graphButton = view.findViewById(R.id.graph_button)
        deleteButton = view.findViewById(R.id.delete_all_locations)

        positionButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToPositionPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        graphButton.setOnClickListener { v ->
            val destinationTab = HistoryDirections.actionHistoryPageToGraphPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }

        deleteButton.setOnClickListener { v ->
            runBlocking {
                launch{
                    dbManager.deleteOld()
                }
            }
//            referenceLocationRepo.deleteAll()
            Snackbar.make(v, R.string.delete_toast, LENGTH_SHORT).show()
//            Toast.makeText(context, R.string.delete_toast, LENGTH_SHORT).show()
//            super.onCreateView(inflater, container, savedInstanceState)
            val destinationTab = HistoryDirections.actionHistoryPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }
//        }

        var allLocations : List<LocationEntity>?
        val elementNum: TextView? = view?.findViewById(R.id.number_element_label)
//        val allLocations = referenceLocationRepo.allLocations.value                                 //TODO Da vedere se si spacca con .value

        val recyclerView: RecyclerView = view.findViewById(R.id.locations_list)

        runBlocking {
            elementNum?.text = getString(R.string.loading_details)
            launch{
                allLocations = dbManager.getAllLocations()
                recyclerView.adapter = LocationAdapter(allLocations!!)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    elementNum?.text = Html.fromHtml(getString(R.string.num_of_element, allLocations?.size), FROM_HTML_MODE_LEGACY)
                else
                    elementNum?.text = getString(R.string.num_of_element, allLocations?.size)
            }
        }

//        elementNum?.text = getString(R.string.num_of_element, allLocations.size)
        return view
    }
}