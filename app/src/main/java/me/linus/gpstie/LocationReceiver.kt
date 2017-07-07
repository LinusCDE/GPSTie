package me.linus.gpstie

import android.location.Location

/**
 * Interface that states that a given class can receive Location- and Status-Updates
 */
interface LocationReceiver {

    fun updateLocation(location: Location)
    fun resetLocation()

    fun updateStatus(status: Int)
    fun resetStatus()

}