package me.linus.gpstie.activity.gpssender

import android.os.Bundle
import me.linus.gpstie.MyDefaultMenuActivity
import me.linus.gpstie.R

class ActivityGPSSender: MyDefaultMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gt_gpssender_layout)

        supportActionBar?.title = "GPS-Sender"
    }

}