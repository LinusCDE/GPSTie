package me.linus.gpstie

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

open class MyDefaultMenuActivity: AppCompatActivity() {

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_default, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item?.itemId) {
            R.id.menu_reassign -> {
                val newIntent = Intent(this, ActivityGPSRoleSelectDialog::class.java)
                newIntent.putExtra("resetDefSelection", true)
                startActivity(newIntent)
                finish()
            }
            else -> return false
        }
        return true
    }

}
