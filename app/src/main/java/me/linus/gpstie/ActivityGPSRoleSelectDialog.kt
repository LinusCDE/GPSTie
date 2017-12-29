package me.linus.gpstie

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import me.linus.gpstie.gpsreceiver.ActivityGPSReceiver
import me.linus.gpstie.gpssender.ActivityGPSSender
import ru.noties.markwon.Markwon

class ActivityGPSRoleSelectDialog : AppCompatActivity() {

    companion object {
        val PRIVACY_POLICY_VERSION = BuildConfig.PRIVACY_POLICY_VERSION

        val SELECTION_GPS_RECEIVER = 1
        val SELECTION_GPS_SENDER = 2
    }

    lateinit var prefs: SharedPreferences // Saves selected Activity in case of "Do not ask again"

    // UI-Elements:
    lateinit var uiReceiverBtn: LinearLayout
    lateinit var uiSenderBtn: LinearLayout
    lateinit var uiDontAskAgainCb: CheckBox
    // --------------------

    /**
     * UI-Setup and check for selected Activity
     */
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


        dataProtectionApprovalCheck({
            // Check for saved value
            if(prefs.contains("defSelection")) {
                println("Status: " + prefs.getInt("defSelection", -1))
                when (prefs.getInt("defSelection", 0)) {
                    SELECTION_GPS_RECEIVER -> selectGpsReceiver()
                    SELECTION_GPS_SENDER -> selectGpsSender()
                }
            }
        })

    }

    /**
     * Opens ActivityGPSReceiver and saves selection if desired
     */
    fun selectGpsReceiver() {
        // Set default value if needed
        if(uiDontAskAgainCb.isChecked)
            prefs.edit().apply { putInt("defSelection", SELECTION_GPS_RECEIVER) }.apply()

        startActivity(Intent(this, ActivityGPSReceiver::class.java)) // Start new activity
        finish() // Close this activity
    }

    /**
     * Opens ActivityGPSSender and saves selection if desired
     */
    fun selectGpsSender() {
        // Set default value if needed
        if(uiDontAskAgainCb.isChecked)
            prefs.edit().apply { putInt("defSelection", SELECTION_GPS_SENDER) }.apply()

        startActivity(Intent(this, ActivityGPSSender::class.java)) // Start new activity
        finish() // Close this activity
    }

    /**
     * Show data protection
     * Returns true if the user hasn't accepted (this version), yet.
     */
    fun dataProtectionApprovalCheck(onAccepted: () -> Unit, onDenied: () -> Unit = { finish() }) {
        if (prefs.getInt("privacy_policy_accepted", -1) == PRIVACY_POLICY_VERSION) {
            onAccepted()
            return
        }

        val builder = AlertDialog.Builder(this)

        // Add custom TextView with margin:
        val attr = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        attr.setMargins(30, 10, 30, 0)
        val linearLayout = LinearLayout(this)

        val content = TextView(this)
        content.layoutParams = attr
        linearLayout.addView(content)
        builder.setView(linearLayout)

        // Load data privacy text as Markdown:
        val dataPrivacyReader = resources.openRawResource(R.raw.privacy_policy).bufferedReader()
        dataPrivacyReader.readLine() // Discard first line (should be the title)
        val text = dataPrivacyReader.readText().trim()
        Markwon.setMarkdown(content, text)

        builder.setTitle(R.string.gt_grs_data_privacy_title)

        // Accept button:
        builder.setPositiveButton(R.string.gt_grs_data_privacy_button_agree, { _, _ ->
            prefs.edit().apply { putInt("privacy_policy_accepted", PRIVACY_POLICY_VERSION) }.apply()
            onAccepted()
        })

        // Deny/quit button:
        builder.setNegativeButton(R.string.gt_grs_data_privacy_button_quit_app, { _, _ ->
            onDenied()
        })

        builder.setCancelable(false)
        builder.show()
    }
}
