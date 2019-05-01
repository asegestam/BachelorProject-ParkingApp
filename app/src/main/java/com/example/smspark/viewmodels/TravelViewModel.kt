package com.example.smspark.viewmodels

import androidx.lifecycle.ViewModel
import com.example.smspark.model.travelmodel.TravelCalc

class TravelViewModel(private val calc: TravelCalc): ViewModel() {

    fun getTotalTravelTime(drivingDuration: Double, walkingDuration: Double) = calc.getTotalTravelTime(drivingDuration, walkingDuration)

    fun getArrivalTime(drivingDuration: Double, walkingDuration: Double) = calc.getArrivalTime(drivingDuration, walkingDuration)

    fun getDrivingDistance(drivingDistance: Double) = calc.getDrivingDistance(drivingDistance)

    fun getDrivingTime(drivingDuration: Double) = calc.getDrivingTime(drivingDuration)

    fun getWalkingDistance(walkingDistance: Double) = calc.getWalkingDistance(walkingDistance)

    fun getWalkingTime(walkingDuration: Double) = calc.getWalkingTime(walkingDuration)
}