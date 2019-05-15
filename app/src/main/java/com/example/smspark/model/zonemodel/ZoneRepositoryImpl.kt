package com.example.smspark.model.zonemodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.smspark.model.extentionFunctions.changeValue
import com.google.gson.GsonBuilder
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import org.koin.core.KoinComponent
import org.koin.core.inject

@SuppressLint("LogNotTimber")
class ZoneRepositoryImpl: ZoneRepository, KoinComponent {

    private val TAG  = "ZoneRepositoryImpl"
    private val service: ZoneService by inject()

    override fun standardZones(): LiveData<List<Feature>> {
        return standardZones
    }

    override fun accessibleZones(): LiveData<List<Feature>> {
        return accessibleZones
    }

    private val standardZones: MutableLiveData<List<Feature>> by lazy {
        MutableLiveData<List<Feature>>()
    }

    private val accessibleZones: MutableLiveData<List<Feature>> by lazy {
        MutableLiveData<List<Feature>>()
    }

    /** Fetches zones from an REST API around a specific LatLong with a fixes radius
     * @param latitude latitude
     * @param longitude longitude
     * @param radius radius from the LatLong to fetch zones
     * */
    override suspend fun getSpecificZones(latitude: Double, longitude: Double, radius: Int, getAccessible: Boolean){
        val result = service.getSpecificZones(latitude, longitude, radius).await()
        if(result.isSuccessful) {
            val zones = result.body()
            val gson = GsonBuilder().setLenient().create()
            val featuresJson = gson.toJson(zones)
            //create a FeatureCollection of the given respone
            val featureCollection = FeatureCollection.fromJson(featuresJson)
            Log.d("onResponse", featureCollection.features().toString())
            standardZones.changeValue(featureCollection.features()!!)
            if(getAccessible) getAccessibleZones(latitude, longitude, radius)
        }  else Log.e("ZoneRepository", "Exception ${result.code()}")
    }

    /** Fetches zones from an REST API around a specific LatLong with a fixes radius
     * @param latitude latitude
     * @param longitude longitude
     * @param radius radius from the LatLong to fetch zones
     * */
    override suspend fun getAccessibleZones(latitude: Double, longitude: Double, radius: Int) {
        val result = service.getHandicapZones(latitude, longitude, radius).await()
        if(result.isSuccessful) {
            val zones = result.body()
            if (!zones.isNullOrEmpty()) {
                Log.d("onRespone", "Hanidcap respone")
                val features = ArrayList<Feature>()
                //for each Handicap object, create a feature and add it to a collection
                zones.forEach {
                    val feature = Feature.fromGeometry(Point.fromLngLat(it.long, it.lat))
                    feature.apply {
                        addStringProperty("id", it.id)
                        addStringProperty("zone_name", it.name)
                        addStringProperty("zone_owner", it.owner)
                        addNumberProperty("parking_spaces", it.parkingSpaces)
                        addStringProperty("max_parking_time", it.maxParkingTime)
                        addStringProperty("max-parking_time_limitation", it.maxParkingTimeLimitation)
                        addStringProperty("extra_info", it.extraInfo)
                        addNumberProperty("distance", it.distance)
                        addNumberProperty("lat", it.lat)
                        addNumberProperty("long", it.long)
                        addStringProperty("wkt", it.WKT)
                    }
                    features.add(feature)
                }
                accessibleZones.changeValue(features)
            } else {
                accessibleZones.changeValue(emptyList())
            }
        } else Log.e("ZoneRepository", "Exception ${result.code()}")
    }

    override fun clearAccessibleZones() {
        this.accessibleZones.value = emptyList()
    }
}
