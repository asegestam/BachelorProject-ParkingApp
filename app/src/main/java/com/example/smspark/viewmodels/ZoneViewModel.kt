package com.example.smspark.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.zonemodel.ZoneRepository
import com.mapbox.geojson.Feature

class ZoneViewModel(private val repo: ZoneRepository): ViewModel(){

    fun getStandardZones(): LiveData<List<Feature>> = repo.getStandardZones()

    fun getAccessibleZones(): LiveData<List<Feature>> = repo.getAccessibleZones()

    fun getSpecificZones(latitude: Double = 57.7089, longitude: Double = 11.9746, radius: Int = 500, fetchAccessible: Boolean = false){
        repo.getSpecificZones(latitude, longitude, radius, fetchAccessible)
    }
}
