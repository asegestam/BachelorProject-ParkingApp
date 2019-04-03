package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.Feature
import com.example.smspark.model.ZoneRepository

class SelectedZoneViewModel(private val repo: ZoneRepository): ViewModel() {


    val selectedMapZone: MutableLiveData<com.mapbox.geojson.Feature?> by lazy {
        MutableLiveData<com.mapbox.geojson.Feature?>()
    }

    val selectedListZone: MutableLiveData<Feature> by lazy {
        MutableLiveData<Feature>()
    }
}