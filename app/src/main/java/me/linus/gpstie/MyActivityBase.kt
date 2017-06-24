package me.linus.gpstie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

/**
 * This activity does:
 *  - Creating default menu and using it
 *  - Adding a WakeLock to control sleep state of this activity
 */
open class MyActivityBase : AppCompatActivity() {

    // Controls wether this activity can put into Sleep mode
    lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GPS-Tie AntiSleep");
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_default, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item?.itemId) {
            R.id.menu_reassign -> {
                returnToMainActivity()
            }
            else -> return false
        }
        return true
    }

    /**
     * Returns to GPSRoleSelectDialog and removes the "Do not ask again"-Tick
     */
    fun returnToMainActivity() {
        val newIntent = Intent(this, ActivityGPSRoleSelectDialog::class.java)
        newIntent.putExtra("resetDefSelection", true)
        startActivity(newIntent)
        finish()
    }

}
