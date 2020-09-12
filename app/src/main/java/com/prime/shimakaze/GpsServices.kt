package com.prime.shimakaze


import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.GpsStatus
import android.location.GpsStatus.Listener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.ContextCompat

class GpsServices : Service(), LocationListener, GpsStatus.Listener {
    private var mLocationManager: LocationManager? = null

    internal var lastlocation = Location("last")
    internal var data: Data? = null

    internal var currentLon = 0.0
    internal var currentLat = 0.0
    internal var lastLon = 0.0
    internal var lastLat = 0.0

    internal var contentIntent: PendingIntent? = null


    override fun onCreate() {

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        contentIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 0)

        updateNotification(false)

        mLocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocationManager!!.addGpsStatusListener(this)
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        data = MainActivity.data
        if (data!!.isRunning) {
            currentLat = location.latitude
            currentLon = location.longitude

            if (data!!.isFirstTime) {
                lastLat = currentLat
                lastLon = currentLon
                data?.isFirstTime = false
            }

            lastlocation.latitude = lastLat
            lastlocation.longitude = lastLon
            val distance = lastlocation.distanceTo(location).toDouble()

            if (location.accuracy < distance) {
                data?.addDistance(distance)

                lastLat = currentLat
                lastLon = currentLon
            }

            if (location.hasSpeed()) {
                data?.setCurSpeed(location.speed * 3.6)
                if (location.speed == 0f) {
                    isStillStopped().execute()
                }
            }
            data?.update()
            updateNotification(true)
            reportData()
        }
    }
    fun reportData() {

    }
    fun updateNotification(asData: Boolean) {
        val builder = Notification.Builder(baseContext)
                .setContentTitle(getString(R.string.running))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(contentIntent)

        if (asData) {
            builder.setContentText(String.format(getString(R.string.notification), data?.maxSpeed, data?.distance))
        } else {
            builder.setContentText(String.format(getString(R.string.notification), '-', '-'))
        }
//        val notification = builder.build()
//        startForeground(R.string.noti_id, notification)


        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("my_service", "My Background Service")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }

        val notificationBuilder = NotificationCompat.Builder(this, channelId )
        val notification2 = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        startForeground(101, notification2)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // If we get killed, after returning from here, restart
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    /* Remove the locationlistener updates when Services is stopped */
    override fun onDestroy() {
        mLocationManager!!.removeUpdates(this)
        mLocationManager!!.removeGpsStatusListener(this)
        stopForeground(true)
    }

    override fun onGpsStatusChanged(event: Int) {}

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    internal inner class isStillStopped : AsyncTask<Void, Int, String>() {
        var timer = 0

        override fun doInBackground(vararg unused: Void): String {
            try {
                while (data?.getCurSpeed()== 0.0) {
                    Thread.sleep(1000)
                    timer++
                }
            } catch (t: InterruptedException) {
                return "The sleep operation failed"
            }

            return "return object when task is finished"
        }

        override fun onPostExecute(message: String) {
            data?.setTimeStopped(timer.toLong())
        }
    }
}
