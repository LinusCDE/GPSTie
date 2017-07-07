package me.linus.gpstie

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

/**
 * This activity does:
 *  - Creating default menu and using it
 */
open class MyActivityBase : AppCompatActivity() {

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_default, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item?.itemId) {
            R.id.menu_to_roleselect -> {
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
