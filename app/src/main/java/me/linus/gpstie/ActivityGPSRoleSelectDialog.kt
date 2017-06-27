package me.linus.gpstie

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import me.linus.gpstie.activity.gpsreceiver.ActivityGPSReceiver
import me.linus.gpstie.activity.gpssender.ActivityGPSSender

class ActivityGPSRoleSelectDialog : AppCompatActivity() {

    companion object {
        val SELECTION_GPS_RECEIVER = 1
        val SELECTION_GPS_SENDER = 2
    }

    lateinit var prefs: SharedPreferences

    lateinit var uiReceiverBtn: LinearLayout
    lateinit var uiSenderBtn: LinearLayout
    lateinit var uiDontAskAgainCb: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        // Init
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gt_gpsroleselect_layout_dialog)

        setTitle(R.string.gt_grs_title_chooserole)

        // Open Prefs
        prefs = getPreferences(Context.MODE_PRIVATE)

        // Get UI-Elements
        uiReceiverBtn = findViewById(R.id.gt_grs_gpsreceive) as LinearLayout
        uiSenderBtn = findViewById(R.id.gt_grs_gpssend) as LinearLayout

        uiDontAskAgainCb = findViewById(R.id.gt_grs_dontaskagaincb) as CheckBox

        // Setup Listeners
        uiReceiverBtn.setOnClickListener { selectGpsReceiver() }
        uiSenderBtn.setOnClickListener { selectGpsSender()}

        // Check if default selection needs to be reset
        if(intent != null && intent.hasExtra("resetDefSelection") &&
                intent.getBooleanExtra("resetDefSelection", false))
            prefs.edit().run { remove("defSelection") }.apply() // Removes 'defSelection'-Key

        // Check for saved value
        if(prefs.contains("defSelection")) {
            println("Status: " + prefs.getInt("defSelection", -1))
            when (prefs.getInt("defSelection", 0)) {
                SELECTION_GPS_RECEIVER -> selectGpsReceiver()
                SELECTION_GPS_SENDER -> selectGpsSender()
            }
        }
    }

    fun selectGpsReceiver() {
        // Set default value if needed
        if(uiDontAskAgainCb.isChecked)
            prefs.edit().apply { putInt("defSelection", SELECTION_GPS_RECEIVER) }.apply()

        // Start new activity
        startActivity(Intent(this, ActivityGPSReceiver::class.java))
        // Close this activity
        finish()
    }

    fun selectGpsSender() {
        // Set default value if needed
        if(uiDontAskAgainCb.isChecked)
            prefs.edit().apply { putInt("defSelection", SELECTION_GPS_SENDER) }.apply()

        // Start new activity
        startActivity(Intent(this, ActivityGPSSender::class.java))
        // Close this activity
        finish()
    }
}
