package me.linus.gpstie.activity.gpsreceiver

import android.os.Bundle
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.R

class ActivityGPSReceiver: MyActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gt_gpsreceiver_layout)

        supportActionBar?.title = "GPS-Receiver"
    }

}