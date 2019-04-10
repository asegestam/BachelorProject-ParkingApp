package com.example.smspark.model.ZoneModel

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
    val zoneFeatures = MutableLiveData<FeatureCollection>()
    val handicapPoints = MutableLiveData<FeatureCollection>()

    /** Uses a retrofit service to fetch parking zones near given coordinate and take the response to build a FeatureCollection
     * @param latitude
     * @param longitude
     * @param radius radius from the lat/long to fetch zones
     *
     */
    override fun getSpecificZones(latitude: Double, longitude: Double, radius: Int): LiveData<FeatureCollection> {

        val call = service.getSpecificZones(latitude, longitude, radius)
        //val call = service.getZones()
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
                    zoneFeatures.value = featureCollection
                }
            }
        })
        return zoneFeatures
    }

    override fun getHandicapZones(): MutableLiveData<FeatureCollection> {
        val call = service.getHandicapZones()
        call.enqueue(object : retrofit2.Callback<List<Handicap>> {

            override fun onFailure(call: Call<List<Handicap>>, t: Throwable) {
                Log.e(TAG, t.message)
            }

            override fun onResponse(call: Call<List<Handicap>>, response: Response<List<Handicap>>) {
                if(response.isSuccessful) {
                    val zones = response.body()!!
                    val features = ArrayList<Feature>()
                    //for each Handicap object, create a feature and add it to a collection
                    zones.forEach {
                        val feature = Feature.fromGeometry(Point.fromLngLat(it.long, it.lat))
                        feature.addStringProperty("Id", it.id)
                        feature.addStringProperty("Name", it.name)
                        feature.addStringProperty("Owner", it.owner)
                        feature.addNumberProperty("ParkingSpaces", it.parkingSpaces)
                        feature.addStringProperty("MaxParkingTime", it.maxParkingTime)
                        feature.addStringProperty("MaxParkingTimeLimitation", it.maxParkingTimeLimitation)
                        feature.addStringProperty("ExtraInfo", it.extraInfo)
                        feature.addNumberProperty("Distance", it.distance)
                        feature.addNumberProperty("Lat", it.lat)
                        feature.addNumberProperty("Long", it.long)
                        feature.addStringProperty("WKT", it.WKT)
                        features.add(feature)
                    }
                    Timber.d("Handicap call to GBGSTAD parsed: " + handicapPoints.value?.features()?.toString())
                    handicapPoints.value = FeatureCollection.fromFeatures(features)
                }
            }
        })
        return handicapPoints
    }

    override fun getZones(): LiveData<FeatureCollection> {

        val call = service.getZones()
        //val call = service.getZones()
        call.enqueue(object : retrofit2.Callback<Zone> {
            override fun onFailure(call: Call<Zone>, t: Throwable) {
                Log.e(TAG, t.message)
            }

            override fun onResponse(call: Call<Zone>, response: Response<Zone>) {
                if(response.isSuccessful) {
                    val response = response.body()
                }
            }
        })
        return zoneFeatures
    }
}
