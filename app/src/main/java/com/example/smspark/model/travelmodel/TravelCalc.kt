package com.example.smspark.model.travelmodel

interface TravelCalc {

    fun getTotalTravelTime(drivingDuration: Double, walkingDuration: Double): String

    fun getArrivalTime(drivingDuration: Double, walkingDuration: Double): String

    fun getDrivingDistance(drivingDistance: Double): String

    fun getDrivingTime(drivingDuration: Double): String

    fun getWalkingDistance(walkingDistance: Double): String

    fun getWalkingTime(walkingDuration: Double): String

}