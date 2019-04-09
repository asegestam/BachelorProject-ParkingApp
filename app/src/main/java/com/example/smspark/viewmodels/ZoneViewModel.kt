package com.example.smspark.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.ZoneModel.ZoneRepository
import com.mapbox.geojson.FeatureCollection

class ZoneViewModel(private val repo: ZoneRepository): ViewModel(){

    fun getSpecificZones(latitude: Double = 57.7089, longitude: Double = 11.9746, radius: Int = 500) : LiveData<FeatureCollection> {
        return repo.getSpecificZones(latitude, longitude, radius)
    }

    fun getZones() : LiveData<FeatureCollection> {
        return repo.getZones()
    }

    fun getHandicapZones() : LiveData<FeatureCollection> {
        return repo.getHandicapZones()
    }
}
