package com.prime.shimakaze

class Data() {
    var isRunning: Boolean = false
    var time: Long = 0
    private var timeStopped: Long = 0
    var isFirstTime: Boolean = false

    var distance: Double = 0.toDouble()
    private var curSpeed: Double = 0.toDouble()
    var maxSpeed: Double = 0.toDouble()

    private var onGpsServiceUpdate: OnGpsServiceUpdate? = null

    val averageSpeed: Double
        get() {
            val average: Double
            val units: String
            if (time <= 0) {
                average = 0.0
            } else {
                average = distance / (time / 1000.0) * 3.6
            }
            return average
        }

    val averageSpeedMotion: Double
        get() {
            val motionTime = time - timeStopped
            val average: Double
            val units: String
            if (motionTime <= 0) {
                average = 0.0
            } else {
                average = distance / (motionTime / 1000.0) * 3.6
            }
            return average
        }

    interface OnGpsServiceUpdate {
        fun update()
    }

    fun setOnGpsServiceUpdate(onGpsServiceUpdate: OnGpsServiceUpdate?) {
        this.onGpsServiceUpdate = onGpsServiceUpdate
    }

    fun update() {
        onGpsServiceUpdate!!.update()
    }

    init {
        isRunning = false
        distance = 0.0
        curSpeed = 0.0
        maxSpeed = 0.0
        timeStopped = 0
    }

    constructor(onGpsServiceUpdate: OnGpsServiceUpdate?) : this() {
        setOnGpsServiceUpdate(onGpsServiceUpdate)
    }

    fun addDistance(distance: Double) {
        this.distance = this.distance + distance
    }

    fun setCurSpeed(curSpeed: Double) {
        this.curSpeed = curSpeed
        if (curSpeed > maxSpeed) {
            maxSpeed = curSpeed
        }
    }

    fun setTimeStopped(timeStopped: Long) {
        this.timeStopped += timeStopped
    }

    fun getCurSpeed(): Double {
        return curSpeed
    }

    override fun toString(): String {
        return super.toString()
    }
}

