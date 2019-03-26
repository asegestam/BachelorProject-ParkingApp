package com.example.smspark.model

import com.google.gson.annotations.SerializedName

data class Handicap(
        @SerializedName("Id")
        val id: String,
        @SerializedName("Name")
        val name: String,
        @SerializedName("Owner")
        val owner: String,
        @SerializedName("ParkingSpaces")
        val parkingSpaces: Int,
        @SerializedName("MaxParkingTime")
        val maxParkingTime: String,
        @SerializedName("MaxParkingTimeLimitation")
        val maxParkingTimeLimitation: String,
        @SerializedName("ExtraInfo")
        val extraInfo: String,
        @SerializedName("Distance")
        val distance: Int,
        @SerializedName("Lat")
        val lat: Double,
        @SerializedName("Long")
        val long: Double,
        @SerializedName("WKT")
        val WKT: String
)
