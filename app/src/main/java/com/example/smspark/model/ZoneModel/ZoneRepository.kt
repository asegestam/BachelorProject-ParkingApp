package com.example.smspark.model.zonemodel

import androidx.lifecycle.LiveData
import com.mapbox.geojson.FeatureCollection

interface ZoneRepository {

    fun getObservableZones() : LiveData<FeatureCollection>

    fun getObservableHandicapZones() : LiveData<FeatureCollection>

    fun getSpecificZones(latitude: Double, longitude: Double, radius: Int)

    fun getHandicapZones(latitude: Double, longitude: Double, radius: Int)
}
