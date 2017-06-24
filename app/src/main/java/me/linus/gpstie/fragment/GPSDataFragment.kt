package me.linus.gpstie.fragment

import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import me.linus.gpstie.R

class GPSDataFragment: Fragment() {

    lateinit var uiGpsLatitude: TextView
    lateinit var uiGpsLongitude: TextView
    lateinit var uiGpsAccuracy: TextView
    lateinit var uiGpsAltitude: TextView
    lateinit var uiGpsBearing: TextView
    lateinit var uiGpsSpeed: TextView
    lateinit var uiGpsProvider: TextView
    lateinit var uiGpsElaspedRealtimeMillis: TextView
    lateinit var uiGpsTime: TextView

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(inflater == null) return null

        val fragView = inflater.inflate(R.layout.gt_gpsinfo_fragment, container, false)

        uiGpsLatitude = fragView.findViewById(R.id.gt_gpsdata_latitude) as TextView
        uiGpsLongitude = fragView.findViewById(R.id.gt_gpsdata_longitude) as TextView
        uiGpsAccuracy = fragView.findViewById(R.id.gt_gpsdata_accuracy) as TextView
        uiGpsAltitude = fragView.findViewById(R.id.gt_gpsdata_altitude) as TextView
        uiGpsBearing = fragView.findViewById(R.id.gt_gpsdata_bearing) as TextView
        uiGpsSpeed = fragView.findViewById(R.id.gt_gpsdata_speed) as TextView
        uiGpsProvider = fragView.findViewById(R.id.gt_gpsdata_provider) as TextView
        uiGpsElaspedRealtimeMillis = fragView.findViewById(R.id.gt_gpsdata_elapsed_realtime_nanos) as TextView
        uiGpsTime = fragView.findViewById(R.id.gt_gpsdata_time) as TextView

        return fragView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun updateLocation(location: Location) =
        activity.runOnUiThread {
            var none = resources.getString(R.string.no_value)

            uiGpsLatitude.text = "${location.latitude}"
            uiGpsLongitude.text = "${location.longitude}"
            uiGpsAccuracy.text = "\u00B1${if(location.hasAccuracy()) "${location.accuracy}" else none}m"
            uiGpsAltitude.text = "${if(location.hasAltitude()) "${location.altitude}" else none}"
            uiGpsBearing.text = "${if(location.hasBearing()) "${location.bearing}" else none}"
            uiGpsSpeed.text = "${if(location.hasSpeed()) "${location.speed}" else none}m/s"
            uiGpsProvider.text = "${location.provider}"
            uiGpsElaspedRealtimeMillis.text = "${location.elapsedRealtimeNanos}"
            uiGpsTime.text = "${location.time}"
        }

}