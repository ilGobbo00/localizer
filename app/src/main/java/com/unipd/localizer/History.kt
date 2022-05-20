package com.unipd.localizer

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class History : Fragment(){
    private lateinit var  positionButton: TextView
    private lateinit var  graphButton: TextView
    private lateinit var  deleteButton: FloatingActionButton

    private lateinit var referenceLocationRepo: ReferenceLocationRepo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        referenceLocationRepo = ViewModelProvider(requireActivity()).get(ReferenceLocationRepo::class.java)
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
            referenceLocationRepo.deleteAll()
            Toast.makeText(context, R.string.delete_toast, LENGTH_SHORT).show()
//            super.onCreateView(inflater, container, savedInstanceState)
            val destinationTab = HistoryDirections.actionHistoryPageToHistoryPage()
            Navigation.findNavController(v).navigate(destinationTab)
        }
//        }

        val allLocations = referenceLocationRepo.allLocations.value                                 //TODO Da vedere se si spacca con .value
        val recyclerView: RecyclerView = view.findViewById(R.id.locations_list)
        recyclerView.adapter = LocationAdapter(allLocations!!)

        val elementNum: TextView? = view?.findViewById(R.id.number_element_label)
//        elementNum?.text = getString(R.string.num_of_element, allLocations.size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            elementNum?.text = Html.fromHtml(getString(R.string.num_of_element, allLocations.size), FROM_HTML_MODE_LEGACY)
        else
            elementNum?.text = getString(R.string.num_of_element, allLocations.size)
        return view
    }
}