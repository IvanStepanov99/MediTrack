package com.example.medtracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medtracker.R

class StatsActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        BottomBarHelper.setup(this, R.id.btnStats)
    }
}
