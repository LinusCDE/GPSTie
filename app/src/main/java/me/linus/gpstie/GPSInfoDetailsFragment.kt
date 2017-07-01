package me.linus.gpstie

import android.location.Location
import android.location.LocationProvider
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import me.linus.gpstie.GpsLocation
import me.linus.gpstie.LocationReceiver
import me.linus.gpstie.R

class GPSInfoDetailsFragment : Fragment(), LocationReceiver {

    lateinit var uiGpsLatitude: TextView
    lateinit var uiGpsLongitude: TextView
    lateinit var uiGpsAccuracy: TextView
    lateinit var uiGpsAltitude: TextView
    lateinit var uiGpsBearing: TextView
    lateinit var uiGpsSpeed: TextView
    lateinit var uiGpsProvider: TextView
    lateinit var uiGpsElaspedRealtimeMillis: TextView
    lateinit var uiGpsTime: TextView
    lateinit var uiGpsStatus: TextView

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(inflater == null) return null

        val fragView = inflater.inflate(R.layout.gt_gpsinfo_frag_details, container, false)

        uiGpsLatitude = fragView.findViewById(R.id.gt_gpsinfo_details_latitude) as TextView
        uiGpsLongitude = fragView.findViewById(R.id.gt_gpsinfo_details_longitude) as TextView
        uiGpsAccuracy = fragView.findViewById(R.id.gt_gpsinfo_details_accuracy) as TextView
        uiGpsAltitude = fragView.findViewById(R.id.gt_gpsinfo_details_altitude) as TextView
        uiGpsBearing = fragView.findViewById(R.id.gt_gpsinfo_details_bearing) as TextView
        uiGpsSpeed = fragView.findViewById(R.id.gt_gpsinfo_details_speed) as TextView
        uiGpsProvider = fragView.findViewById(R.id.gt_gpsinfo_details_provider) as TextView
        uiGpsElaspedRealtimeMillis = fragView.findViewById(R.id.gt_gpsinfo_details_elapsed_realtime_nanos) as TextView
        uiGpsTime = fragView.findViewById(R.id.gt_gpsinfo_details_time) as TextView
        uiGpsStatus = fragView.findViewById(R.id.gt_gpsinfo_details_status) as TextView

        return fragView
    }

    override fun updateLocation(location: Location) {
        activity?.runOnUiThread {

            val none = resources.getString(R.string.no_value)

            uiGpsLatitude.text = "${location.latitude}"
            uiGpsLongitude.text = "${location.longitude}"
            uiGpsAccuracy.text = "\u00B1${if (location.hasAccuracy()) "${location.accuracy}" else none}m"
            uiGpsAltitude.text = if (location.hasAltitude()) "${location.altitude}" else none
            uiGpsBearing.text = if (location.hasBearing()) "${location.bearing}" else none
            uiGpsSpeed.text = "${if (location.hasSpeed()) "${location.speed}" else none}m/s"
            uiGpsProvider.text = location.provider
            uiGpsElaspedRealtimeMillis.text = "${location.elapsedRealtimeNanos}"
            uiGpsTime.text = "${location.time}"
        }
    }

    override fun resetLocation() {
        activity?.runOnUiThread {
            val none = resources.getString(R.string.no_value)

            uiGpsLatitude.text = none
            uiGpsLongitude.text = none
            uiGpsAccuracy.text = none
            uiGpsAltitude.text = none
            uiGpsBearing.text = none
            uiGpsSpeed.text = none
            uiGpsProvider.text = none
            uiGpsElaspedRealtimeMillis.text = none
            uiGpsTime.text = none
        }
    }

    override fun updateStatus(status: Int) {
        activity?.runOnUiThread {
            uiGpsStatus.text = when (status) {
                LocationProvider.AVAILABLE ->
                    resources.getString(R.string.gt_gpsinfo_details_label_gps_available)
                LocationProvider.OUT_OF_SERVICE ->
                    resources.getString(R.string.gt_gpsinfo_details_label_gps_out_of_service)
                LocationProvider.TEMPORARILY_UNAVAILABLE ->
                    resources.getString(R.string.gt_gpsinfo_details_label_gps_temporarily_unavailable)
                else -> resources.getString(R.string.gt_gpsinfo_details_label_unknown, status)
            }
        }
    }

    override fun resetStatus() {
        activity?.runOnUiThread { uiGpsStatus.setText(R.string.no_value) }
    }

}