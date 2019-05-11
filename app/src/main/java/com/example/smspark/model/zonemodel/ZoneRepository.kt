package com.example.smspark.model.zonemodel

import androidx.lifecycle.LiveData
import com.mapbox.geojson.Feature

interface ZoneRepository {

    fun getStandardZones() : LiveData<List<Feature>>

    fun getAccessibleZones() : LiveData<List<Feature>>

    fun getSpecificZones(latitude: Double, longitude: Double, radius: Int)

    fun getAccessibleZones(latitude: Double, longitude: Double, radius: Int)
}
