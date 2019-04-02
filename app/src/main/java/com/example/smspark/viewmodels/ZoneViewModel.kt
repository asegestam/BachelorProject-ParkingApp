package com.example.smspark.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.Feature
import com.example.smspark.model.ZoneRepository
import com.google.gson.GsonBuilder

class ZoneViewModel(val repo: ZoneRepository): ViewModel(){

    val zonePolygons:  MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val zonePoints:  MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val handicapPoints: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val zoneFeatures: MutableLiveData<List<Feature>> by lazy {
        MutableLiveData<List<Feature>>()
    }

    val zoneChosen: MutableLiveData<Feature> by lazy {
        MutableLiveData<Feature>()
    }


    fun getZones() {
        val data = repo.getZones().value
        if (data != null && !data.equals(zoneFeatures.value)) {
            Log.d("ViewModel", "Data not null, changing value on zones")

            val gson = GsonBuilder().setLenient().create()
            //Filter out features that are polygons and points to seperate lists
            val polygonFeatures = data.features.filter { it.geometry.type == "Polygon" }
            val pointFeatures = data.features.filter { it.geometry.type == "Point" }

            val polygons = data.copy()
            val points = data.copy()
            polygons.features = polygonFeatures
            points.features = pointFeatures

            zonePolygons.value = gson.toJson(polygons)
            zonePoints.value = gson.toJson(points)
            zoneFeatures.value = data.features
        }
    }

    fun getHandicapZones(){
        val data = repo.getHandicapZones().value
        if (data != null){
            handicapPoints.value = data
        }
    }
}