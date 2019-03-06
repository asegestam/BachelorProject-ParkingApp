package com.example.smspark.dto

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.GeoJson

data class Parking(var type : String,
                   var features : Array<GeoJson>) {
}