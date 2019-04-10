package com.example.smspark.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.ZoneModel.ZoneRepository
import com.mapbox.geojson.FeatureCollection

class ZoneViewModel(private val repo: ZoneRepository): ViewModel(){

    fun getSpecificZones(latitude: Double, longitude: Double, radius: Int) : LiveData<FeatureCollection> {
        return repo.getSpecificZones(latitude, longitude, radius)
    }

    fun getHandicapZones() : LiveData<FeatureCollection> {
        return repo.getHandicapZones()
    }
}
