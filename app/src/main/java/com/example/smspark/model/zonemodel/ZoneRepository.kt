package com.example.smspark.model.zonemodel

import androidx.lifecycle.LiveData
import com.mapbox.geojson.FeatureCollection

interface ZoneRepository {

    fun getAllZones() : LiveData<HashMap<String,FeatureCollection>>

    fun getSpecificZones(latitude: Double, longitude: Double, radius: Int)

    fun getHandicapZones(latitude: Double, longitude: Double, radius: Int)
}
