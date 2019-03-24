package com.example.smspark.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.ZoneRepository

class ZoneViewModel(val repo: ZoneRepository): ViewModel(){

    val zonePolygons:  MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val zonePoints:  MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun getZones() {
        val data = repo.getZones().value
        if(data != null) {
            Log.d("ViewModel" , "Data not null, changing value on zones")
            zonePolygons.value = data.getValue("polygons")
            zonePoints.value = data.getValue("points")
        }
    }

}