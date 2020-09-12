package com.prime.shimakaze

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.GpsSatellite
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.SystemClock
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Chronometer
import android.widget.TextView

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate
import com.gc.materialdesign.widgets.Dialog
import com.google.gson.Gson
import com.melnykov.fab.FloatingActionButton
import com.prime.shimakaze.R

import java.util.Locale


class MainActivity : AppCompatActivity(), LocationListener, GpsStatus.Listener {

    private var sharedPreferences: SharedPreferences? = null
    private var mLocationManager: LocationManager? = null

    private var toolbar: Toolbar? = null
    private var fab: FloatingActionButton? = null
    private var refresh: FloatingActionButton? = null
    private var progressBarCircularIndeterminate: ProgressBarCircularIndeterminate? = null
    private var satellite: TextView? = null
    private var status: TextView? = null
    private var accuracy: TextView? = null
    private var currentSpeed: TextView? = null
    private var maxSpeed: TextView? = null
    private var averageSpeed: TextView? = null
    private var distance: TextView? = null
    private var time: Chronometer? = null
    private var onGpsServiceUpdate: Data.OnGpsServiceUpdate? = null

    private var firstfix: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        data = Data(onGpsServiceUpdate)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        //setTitle("");
        fab = findViewById(R.id.fab) as FloatingActionButton
        fab!!.visibility = View.INVISIBLE

        refresh = findViewById(R.id.refresh) as FloatingActionButton
        refresh!!.visibility = View.INVISIBLE

        onGpsServiceUpdate = object : Data.OnGpsServiceUpdate {
            override fun update() {
                var maxSpeedTemp = data!!.maxSpeed
                var distanceTemp = data!!.distance
                var averageTemp: Double
                if (sharedPreferences!!.getBoolean("auto_average", false)) {
                    averageTemp = data!!.averageSpeedMotion
                } else {
                    averageTemp = data!!.averageSpeed
                }

                val speedUnits: String
                val distanceUnits: String
                if (sharedPreferences!!.getBoolean("miles_per_hour", false)) {
                    maxSpeedTemp *= 0.62137119
                    distanceTemp = distanceTemp / 1000.0 * 0.62137119
                    averageTemp *= 0.62137119
                    speedUnits = "mi/h"
                    distanceUnits = "mi"
                } else {
                    speedUnits = "km/h"
                    if (distanceTemp <= 1000.0) {
                        distanceUnits = "m"
                    } else {
                        distanceTemp /= 1000.0
                        distanceUnits = "km"
                    }
                }

                var s = SpannableString(String.format("%.0f %s", maxSpeedTemp, speedUnits))
                s.setSpan(RelativeSizeSpan(0.5f), s.length - speedUnits.length - 1, s.length, 0)
                maxSpeed!!.text = s

                s = SpannableString(String.format("%.0f %s", averageTemp, speedUnits))
                s.setSpan(RelativeSizeSpan(0.5f), s.length - speedUnits.length - 1, s.length, 0)
                averageSpeed!!.text = s

                s = SpannableString(String.format("%.3f %s", distanceTemp, distanceUnits))
                s.setSpan(RelativeSizeSpan(0.5f), s.length - distanceUnits.length - 1, s.length, 0)
                distance!!.text = s
            }
        }

        mLocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        satellite = findViewById(R.id.satellite) as TextView
        status = findViewById(R.id.status) as TextView
        accuracy = findViewById(R.id.accuracy) as TextView
        maxSpeed = findViewById(R.id.maxSpeed) as TextView
        averageSpeed = findViewById(R.id.averageSpeed) as TextView
        distance = findViewById(R.id.distance) as TextView
        time = findViewById(R.id.time) as Chronometer
        currentSpeed = findViewById(R.id.currentSpeed) as TextView
        progressBarCircularIndeterminate = findViewById(R.id.progressBarCircularIndeterminate) as ProgressBarCircularIndeterminate
        time!!.text = initialTimer
        time!!.onChronometerTickListener = object : Chronometer.OnChronometerTickListener {
            internal var isPair = true
            override fun onChronometerTick(chrono: Chronometer) {
                val time: Long
                if (data!!.isRunning) {
                    time = SystemClock.elapsedRealtime() - chrono.base
                    data!!.time = time
                } else {
                    time = data!!.time
                }

                val h = (time / 3600000).toInt()
                val m = (time - h * 3600000).toInt() / 60000
                val s = (time - (h * 3600000).toLong() - (m * 60000).toLong()).toInt() / 1000
                val hh = if (h < 10) "0$h" else h.toString() + ""
                val mm = if (m < 10) "0$m" else m.toString() + ""
                val ss = if (s < 10) "0$s" else s.toString() + ""
                val displayTimer = "$hh:$mm:$ss"
                chrono.text = displayTimer

                if (data!!.isRunning) {
                    chrono.text = displayTimer
                } else {
                    if (isPair) {
                        isPair = false
                        chrono.text = displayTimer
                    } else {
                        isPair = true
                        chrono.text = ""
                    }
                }

            }
        }
    }

    fun onFabClick(v: View) {
        if (!data!!.isRunning) {
            fab!!.setImageDrawable(resources.getDrawable(R.drawable.ic_action_pause))
            data!!.isRunning = true
            time!!.base = SystemClock.elapsedRealtime() - data!!.time
            time!!.start()
            data!!.isFirstTime = true
            startService(Intent(baseContext, GpsServices::class.java))
            refresh!!.visibility = View.INVISIBLE
        } else {
            fab!!.setImageDrawable(resources.getDrawable(R.drawable.ic_action_play))
            data!!.isRunning = false
            status!!.text = ""
            stopService(Intent(baseContext, GpsServices::class.java))
            refresh!!.visibility = View.VISIBLE
        }
    }

    fun onRefreshClick(v: View) {
        resetData()
        stopService(Intent(baseContext, GpsServices::class.java))
    }

    override fun onResume() {
        super.onResume()
        firstfix = true
        if (!data!!.isRunning) {
            val gson = Gson()
            val json = sharedPreferences!!.getString("data", "")
            data = gson.fromJson<Data>(json, Data::class.java)
        }
        if (data == null) {
            data = Data(onGpsServiceUpdate)
        } else {
            data!!.setOnGpsServiceUpdate(onGpsServiceUpdate)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
            )
        }

        if (mLocationManager!!.allProviders.indexOf(LocationManager.GPS_PROVIDER) >= 0) {
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0f, this)
        } else {
            Log.w("MainActivity", "No GPS location provider found. GPS data display will not be available.")
        }

        if (!mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledDialog()
        }

        mLocationManager!!.addGpsStatusListener(this)
    }

    override fun onPause() {
        super.onPause()
        mLocationManager!!.removeUpdates(this)
        mLocationManager!!.removeGpsStatusListener(this)
        val prefsEditor = sharedPreferences!!.edit()
        val gson = Gson()
        val json = gson.toJson(data)
        prefsEditor.putString("data", json)
        //        prefsEditor.commit();
        prefsEditor.apply()
    }

    public override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(baseContext, GpsServices::class.java))
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onLocationChanged(location: Location) {
        if (location.hasAccuracy()) {
            var acc = location.accuracy.toDouble()
            val units: String
            if (sharedPreferences!!.getBoolean("miles_per_hour", false)) {
                units = "ft"
                acc *= 3.28084
            } else {
                units = "m"
            }
            val s = SpannableString(String.format("%.0f %s", acc, units))
            s.setSpan(RelativeSizeSpan(0.75f), s.length - units.length - 1, s.length, 0)
            accuracy!!.text = s

            if (firstfix) {
                status!!.text = ""
                fab!!.visibility = View.VISIBLE
                if (!data!!.isRunning && !TextUtils.isEmpty(maxSpeed!!.text)) {
                    refresh!!.visibility = View.VISIBLE
                }
                firstfix = false
            }
        } else {
            firstfix = true
        }

        if (location.hasSpeed()) {
            progressBarCircularIndeterminate!!.visibility = View.GONE
            var speed = location.speed * 3.6
            val units: String
            if (sharedPreferences!!.getBoolean("miles_per_hour", false)) { // Convert to MPH
                speed *= 0.62137119
                units = "mi/h"
            } else {
                units = "km/h"
            }
            val s = SpannableString(String.format(Locale.ENGLISH, "%.0f %s", speed, units))
            s.setSpan(RelativeSizeSpan(0.25f), s.length - units.length - 1, s.length, 0)
            currentSpeed!!.text = s
        }

    }

    override fun onGpsStatusChanged(event: Int) {
        when (event) {
            GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            1
                    )
                }
                val gpsStatus = mLocationManager!!.getGpsStatus(null)
                var satsInView = 0
                var satsUsed = 0
                val sats = gpsStatus!!.satellites
                for (sat in sats) {
                    satsInView++
                    if (sat.usedInFix()) {
                        satsUsed++
                    }
                }
                satellite!!.text = "$satsUsed/$satsInView"
                if (satsUsed == 0) {
                    fab!!.setImageDrawable(resources.getDrawable(R.drawable.ic_action_play))
                    data!!.isRunning = false
                    status!!.text = ""
                    stopService(Intent(baseContext, GpsServices::class.java))
                    fab!!.visibility = View.INVISIBLE
                    refresh!!.visibility = View.INVISIBLE
                    accuracy!!.text = ""
                    status!!.text = resources.getString(R.string.waiting_for_fix)
                    firstfix = true
                }
            }

            GpsStatus.GPS_EVENT_STOPPED -> if (!mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGpsDisabledDialog()
            }
            GpsStatus.GPS_EVENT_FIRST_FIX -> {
            }
        }
    }

    fun showGpsDisabledDialog() {
        val dialog = Dialog(this, resources.getString(R.string.gps_disabled), resources.getString(R.string.please_enable_gps))

        dialog.setOnAcceptButtonClickListener { startActivity(Intent("android.settings.LOCATION_SOURCE_SETTINGS")) }
        dialog.show()
    }

    fun resetData() {
        fab!!.setImageDrawable(resources.getDrawable(R.drawable.ic_action_play))
        refresh!!.visibility = View.INVISIBLE
        time!!.stop()
        maxSpeed!!.text = ""
        averageSpeed!!.text = ""
        distance!!.text = ""
        time!!.text = initialTimer
        data = Data(onGpsServiceUpdate)
    }

    override fun onBackPressed() {
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}

    override fun onProviderEnabled(s: String) {}

    override fun onProviderDisabled(s: String) {}

    companion object {
        var data: Data? = null
        private val initialTimer = "00:00:00"
    }
}
