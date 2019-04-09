package com.example.smspark.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.ZoneModel.ZoneRepository
import com.google.gson.GsonBuilder
import com.example.smspark.model.Feature
import com.example.smspark.model.Zone
import com.example.smspark.model.ZoneRepository
import com.mapbox.geojson.FeatureCollection

class ZoneViewModel(private val repo: ZoneRepository): ViewModel(){

    fun getSpecificZones(latitude: Double, longitude: Double, radius: Int) : LiveData<Zone> {
        return repo.getSpecificZones(latitude, longitude, radius)
    }

    fun getZones() : LiveData<Zone> {
        return repo.getZones()
    }

    fun getHandicapZones() : LiveData<String> {
        return repo.getHandicapZones()
    }
}
