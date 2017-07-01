package me.linus.gpstie

import android.location.Location

interface LocationReceiver {

    fun updateLocation(location: Location)
    fun resetLocation()

    fun updateStatus(status: Int)
    fun resetStatus()

}