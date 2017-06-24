package me.linus.gpstie.activity.gpsreceiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import me.linus.gpstie.MyActivityBase
import me.linus.gpstie.R

class ActivityGPSReceiver: MyActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gt_gpsreceiver_layout)

        supportActionBar?.title = "GPS-Receiver"

        // Shouldn't be necessary since BLUETOOTH is granted by default. But just to test :)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val btPermStatus = checkSelfPermission(Manifest.permission.BLUETOOTH)
            when(btPermStatus) {
                PackageManager.PERMISSION_GRANTED -> {
                    // _TO_NOT_DO_ANYMORE: Open BT
                }
                PackageManager.PERMISSION_DENIED -> {
                    Toast.makeText(this, "Bluetooth ist unverzeichtbar!", Toast.LENGTH_LONG)
                    finish()
                }
            }
        }
    }

}