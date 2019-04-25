package com.example.smspark.model.zonemodel

import com.google.gson.annotations.SerializedName

data class Zone(
        @SerializedName("type")
        val type: String,
        @SerializedName("features")
        var features: List<Feature>
    )

data class Feature(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String,
        @SerializedName("geometry")
        val geometry: Geometry,
        @SerializedName("properties")
        val properties: Properties
)


data class Geometry (
        @SerializedName("type")
        val type: String,
        @SerializedName("coordinates")
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
