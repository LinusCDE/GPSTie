package me.linus.gpstie

import me.linus.gpstie.GpsLocation

interface LocationReceiver {

    fun updateLocation(location: GpsLocation)
    fun resetLocation()

    fun updateStatus(status: Int)
    fun resetStatus()

}