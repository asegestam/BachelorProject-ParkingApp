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
    val zoneFeatures = MutableLiveData<FeatureCollection>()
    val handicapPoints = MutableLiveData<FeatureCollection>()

    override fun getObservableZones() : LiveData<FeatureCollection>{
        return zoneFeatures
    }

    override fun getObservableHandicapZones(): LiveData<FeatureCollection> {
        return handicapPoints
    }

    /** Fetches zones from an REST API around a specific LatLong with a fixes radius
     * @param latitude latitude
     * @param longitude longitude
     * @param radius radius from the LatLong to fetch zones
     * */
    override fun getSpecificZones(latitude: Double, longitude: Double, radius: Int){

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
                    val features = ArrayList<Feature>()
                    //for each Handicap object, create a feature and add it to a collection
                    zones.forEach {
                        val feature = Feature.fromGeometry(Point.fromLngLat(it.long, it.lat))
                        feature.addStringProperty("id", it.id)
                        feature.addStringProperty("zone_name", it.name)
                        feature.addStringProperty("zone_owner", it.owner)
                        feature.addNumberProperty("parking_spaces", it.parkingSpaces)
                        feature.addStringProperty("max_parking_time", it.maxParkingTime)
                        feature.addStringProperty("max-parking_time_limitation", it.maxParkingTimeLimitation)
                        feature.addStringProperty("extra_info", it.extraInfo)
                        feature.addNumberProperty("distance", it.distance)
                        feature.addNumberProperty("lat", it.lat)
                        feature.addNumberProperty("long", it.long)
                        feature.addStringProperty("wkt", it.WKT)
                        features.add(feature)
                    }
                    Timber.d("Handicap call to GBGSTAD parsed: " + handicapPoints.value?.features()?.toString())
                    handicapPoints.value = FeatureCollection.fromFeatures(features)
                }
            }
        })
    }
}
