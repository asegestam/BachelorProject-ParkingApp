package com.example.smspark.model.travelmodel

import org.koin.core.KoinComponent
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TravelCalcImpl: TravelCalc, KoinComponent {

    override fun getTotalTravelTime(drivingDuration: Double, walkingDuration: Double) = formatTime(drivingDuration + walkingDuration)

    override fun getArrivalTime(drivingDuration: Double, walkingDuration: Double): String {
        val arrivalTime = Calendar.getInstance()
        arrivalTime.add(Calendar.MINUTE, (TimeUnit.SECONDS.toMinutes((drivingDuration + walkingDuration).toLong()).toInt()))
        return SimpleDateFormat("HH:mm").format(arrivalTime.time)
    }

    override fun getDrivingDistance(drivingDistance: Double) = formatDistance(drivingDistance)

    override fun getDrivingTime(drivingDuration: Double) = formatTime(drivingDuration)

    override fun getWalkingDistance(walkingDistance: Double) = formatDistance(walkingDistance)

    override fun getWalkingTime(walkingDuration: Double) = formatTime(walkingDuration)

    private fun formatTime(duration: Double): String {
        return when {
            duration > 60 -> TimeUnit.SECONDS.toMinutes(duration.toLong()).toInt().toString() + " min"
           // duration > 3600 -> TimeUnit.SECONDS.toHours(duration.toLong()).toString() + " h" + TimeUnit.SECONDS.toMinutes(duration.toLong()).toInt().toString() + " min"
            else -> "$duration s"
        }
    }

    private fun formatDistance(distance: Double) = if(distance > 1000) "%.1f".format(distance/1000) + " km" else "%.1f".format(distance) + " m"

}