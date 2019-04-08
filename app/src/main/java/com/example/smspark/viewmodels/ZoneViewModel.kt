package com.example.smspark.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.ZoneModel.ZoneRepository
import com.google.gson.GsonBuilder
import com.mapbox.geojson.FeatureCollection

class ZoneViewModel(private val repo: ZoneRepository): ViewModel(){

    val zonePolygons:  MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val zonePoints:  MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val handicapPoints: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val zoneFeatures: MutableLiveData<FeatureCollection> by lazy {
        MutableLiveData<FeatureCollection>()
    }

    val handicapFeatures: MutableLiveData<FeatureCollection> by lazy {
        MutableLiveData<FeatureCollection>()
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
            //create a FeatureCollection for all zones to use throughout the app
            val featuresJson = gson.toJson(data)
            val featureCollection = FeatureCollection.fromJson(featuresJson)
            zoneFeatures.value = featureCollection
        }
    }

    fun getHandicapZones(){
        val data = repo.getHandicapZones().value
        if (data != null && !data.equals(handicapFeatures.value)){
            val gson = GsonBuilder().setLenient().setPrettyPrinting().create()
            val featuresJson = gson.toJson(data)
            handicapPoints.value = data
        }
    }
}