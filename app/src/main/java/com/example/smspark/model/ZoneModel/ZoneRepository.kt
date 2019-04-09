package com.example.smspark.model.ZoneModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mapbox.geojson.FeatureCollection

interface ZoneRepository {

    fun getZones() : LiveData<FeatureCollection>

    fun getSpecificZones(latitude: Double, longitude: Double, radius: Int): LiveData<FeatureCollection>

    fun getHandicapZones(): MutableLiveData<FeatureCollection>
}