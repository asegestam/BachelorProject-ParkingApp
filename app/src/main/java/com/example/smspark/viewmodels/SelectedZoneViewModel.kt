package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.ZoneModel.ZoneRepository
import com.mapbox.geojson.Feature

class SelectedZoneViewModel(private val repo: ZoneRepository): ViewModel() {


    val selectedZone: MutableLiveData<Feature> by lazy {
        MutableLiveData<Feature>()
    }

}