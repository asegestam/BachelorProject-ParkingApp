package com.example.smspark.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smspark.model.zonemodel.ZoneRepository
import com.mapbox.geojson.Feature
import kotlinx.coroutines.launch

class ZoneViewModel(private val repo: ZoneRepository): ViewModel(){

    fun standardZones(): LiveData<List<Feature>> = repo.standardZones()

    fun accessibleZones(): LiveData<List<Feature>> = repo.accessibleZones()

    fun getSpecificZones(latitude: Double = 57.7089, longitude: Double = 11.9746, radius: Int = 500, getAccessible: Boolean = false){
        viewModelScope.launch { repo.getSpecificZones(latitude, longitude, radius, getAccessible) }
    }

    fun clearAccessibleZones(){ repo.clearAccessibleZones() }

}
