package com.example.smspark.model.zonemodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import org.koin.core.KoinComponent
import org.koin.core.inject
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber

class ZoneRepositoryImpl: ZoneRepository, KoinComponent {

    private val TAG  = "ZoneRepositoryImpl"
    private val service: ZoneService by inject()

    val allZones: MutableLiveData<HashMap<String,FeatureCollection>> by lazy {
        MutableLiveData<HashMap<String,FeatureCollection>>()
    }

    override fun getAllZones() : LiveData<HashMap<String,FeatureCollection>>{
        return allZones
    }

    /** Fetches zones from an REST API around a specific LatLong with a fixes radius
     * @param latitude latitude
     * @param longitude longitude
     * @param radius radius from the LatLong to fetch zones
     * */
    override fun getSpecificZones(latitude: Double, longitude: Double, radius: Int){

        val call = service.getSpecificZones(latitude, longitude, radius)
        call.enqueue(object : retrofit2.Callback<Zone> {
            override fun onFailure(call: Call<Zone>, t: Throwable) {
                Log.e(TAG, t.message)
            }

            override fun onResponse(call: Call<Zone>, response: Response<Zone>) {
                if(response.isSuccessful) {
                    val zones = response.body()
                    val gson = GsonBuilder().setLenient().create()
                    val featuresJson = gson.toJson(zones)
                    //create a FeatureCollection of the given respone
                    val featureCollection = FeatureCollection.fromJson(featuresJson)
                    Timber.d(featureCollection.toString())
                    if(allZones.value.isNullOrEmpty()) {
                        val map = hashMapOf<String, FeatureCollection>()
                        map["standard"] = featureCollection
                        allZones.value = map
                        getHandicapZones(latitude, longitude, radius)
                        return
                    }
                    allZones.value?.let {
                        if (it.size == 1 || it.size == 0) {
                            it["standard"] = featureCollection
                            allZones.value = it
                            getHandicapZones(latitude, longitude, radius)
                            Log.d(TAG, "size of zone hashmap " + allZones.value?.size)
                            return
                        } else {
                            it.clear()
                            it["standard"] = featureCollection
                            allZones.value = it
                            getHandicapZones(latitude, longitude, radius)
                        }
                    }
                }
            }
        })
    }

    /** Fetches zones from an REST API around a specific LatLong with a fixes radius
     * @param latitude latitude
     * @param longitude longitude
     * @param radius radius from the LatLong to fetch zones
     * */
    override fun getHandicapZones(latitude: Double, longitude: Double, radius: Int) {
        val call = service.getHandicapZones(latitude, longitude, radius)
        call.enqueue(object : retrofit2.Callback<List<Handicap>> {

            override fun onFailure(call: Call<List<Handicap>>, t: Throwable) {
                Log.e(TAG, t.message)
            }

            override fun onResponse(call: Call<List<Handicap>>, response: Response<List<Handicap>>) {
                if(response.isSuccessful) {
                    val zones = response.body()!!
                    if (!zones.isNullOrEmpty()) {
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
                        if (allZones.value.isNullOrEmpty()) {
                            val map = hashMapOf<String, FeatureCollection>()
                            map["accessible"] = FeatureCollection.fromFeatures(features)
                            allZones.value = map
                            return
                        }
                        allZones.value?.let {
                            if (it.size == 1) {
                                it["accessible"] = FeatureCollection.fromFeatures(features)
                                allZones.value = it
                                Log.d(TAG, "size of zone hashmap " + allZones.value?.size)
                                return
                            }
                        }
                    }
                }
            }
        })
    }
}
