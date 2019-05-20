package com.example.smspark.model.zonemodel

import androidx.lifecycle.LiveData
import com.mapbox.geojson.Feature

interface ZoneRepository {

    fun standardZones() : LiveData<List<Feature>>

    fun accessibleZones() : LiveData<List<Feature>>

    suspend fun getSpecificZones(latitude: Double, longitude: Double, radius: Int, getAccessible: Boolean)

    fun clearAccessibleZones()
}
