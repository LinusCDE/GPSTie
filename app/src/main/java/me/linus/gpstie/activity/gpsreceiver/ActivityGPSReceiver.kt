package me.linus.gpstie.activity.gpsreceiver

import android.os.Bundle
import me.linus.gpstie.MyDefaultMenuActivity
import me.linus.gpstie.R

class ActivityGPSReceiver: MyDefaultMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gt_gpsreceiver_layout)

        supportActionBar?.title = "GPS-Receiver"
    }

}