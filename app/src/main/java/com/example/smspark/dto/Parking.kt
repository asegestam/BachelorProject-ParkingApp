package com.example.smspark.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Feature

data class Parking(
        val type: String,
        val features: Array<com.example.smspark.dto.Feature>
    )

data class Feature(
        val id: String,
        val type: String,
        val geometry: Geometry,
        val properties: Properties
)

data class Geometry (
        val type: String,
        val coordinates: Any
)

data class Properties(
        val distance: Double,
        @SerializedName("zonecode")
        val zonecode: Int,
        @SerializedName("zone_id")
        val zone_id: Int,
        @SerializedName("parent_zone_code")
        val parentZoneCode: Any,
        @SerializedName("zone_name")
        val zoneName: String,
        @SerializedName("zone_owner")
        val zoneOwner: String,
        @SerializedName("has_evc")
        val hasEvc: Boolean
)
