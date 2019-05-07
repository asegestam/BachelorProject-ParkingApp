package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Feature

class SelectedZoneViewModel: ViewModel() {

    val selectedZone: MutableLiveData<Feature> by lazy {
        MutableLiveData<Feature>()
    }

}