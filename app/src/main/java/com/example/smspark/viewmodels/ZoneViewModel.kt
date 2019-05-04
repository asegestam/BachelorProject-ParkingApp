package com.example.smspark.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.zonemodel.ZoneRepository
import com.mapbox.geojson.FeatureCollection

class ZoneViewModel(private val repo: ZoneRepository): ViewModel(){


    fun getAllZones() : LiveData<HashMap<String, FeatureCollection>> {
        return repo.getAllZones()
    }

    fun getSpecificZones(latitude: Double = 57.7089, longitude: Double = 11.9746, radius: Int = 500){
        repo.getSpecificZones(latitude, longitude, radius)
    }

    fun getHandicapZones(latitude: Double, longitude: Double, radius: Int) {
        repo.getHandicapZones(latitude, longitude, radius)
    }
}
