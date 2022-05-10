package com.unipd.localizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var  historyButton: TextView
    private  lateinit var  graphButton: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val historyButton: TextView = findViewById(R.id.history_button)
        val graphButton: TextView = findViewById(R.id.graph_button)

//        historyButton.setOnClickListener {
//
//            // TODO Da implementare
//        }
//
//        graphButton.setOnClickListener {
//            // TODO Da implementare
//        }
    }
}