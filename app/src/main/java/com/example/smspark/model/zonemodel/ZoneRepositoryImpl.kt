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

    // Lazy inject on the WebService
    private val service: ZoneService by inject()

    /** Used to store parking zones from SMSPark API */
    private val standardZones: MutableLiveData<List<Feature>> by lazy {
        MutableLiveData<List<Feature>>()
    }
    /** Used to store parking zones from Göteborg API */
    private val accessibleZones: MutableLiveData<List<Feature>> by lazy {
        MutableLiveData<List<Feature>>()
    }

    /** Used to return stored parking zones from SMSPark API */
    override fun standardZones(): LiveData<List<Feature>> {
        return standardZones
    }
    /** Used to return stored parking zones from Göteborg API */
    override fun accessibleZones(): LiveData<List<Feature>> {
        return accessibleZones
    }

    /** Fetches zones from an REST API around a specific LatLong with a fixes radius
     * @param latitude latitude
     * @param longitude longitude
     * @param radius radius from the LatLong to fetch zones
     * */
    override suspend fun getSpecificZones(latitude: Double, longitude: Double, radius: Int, getAccessible: Boolean){
        //fetch all zones from the service and wait for them to return a result
        val standardZoneResult = service.getSpecificZonesAsync(latitude, longitude, radius).await()
        val accessibleZoneResult = service.getHandicapZonesAsync(latitude, longitude, radius).await()
        if(standardZoneResult.isSuccessful && accessibleZoneResult.isSuccessful ) {
            //results is successful, get the result bodies and create list of Features
            val standardZonesBody = standardZoneResult.body()
            val accessibleZonesBody = accessibleZoneResult.body()
            val standardFeatures = createStandardZoneFeatures(standardZonesBody)
            val accessibleFeatures = createAccessibleZoneFeatures(accessibleZonesBody)
            standardZones.changeValue(standardFeatures)
            accessibleZones.changeValue(accessibleFeatures)
        }  else Log.e("ZoneRepository", "Exception ${standardZoneResult.code()}")
    }

    /** Creates a list of Features of given Zone Object */
    private fun createStandardZoneFeatures(responseBody: Zone?): ArrayList<Feature> {
        val gson = GsonBuilder().setLenient().create()
        val featuresJson = gson.toJson(responseBody)
        //create a FeatureCollection of the given respone
        val featureCollection = FeatureCollection.fromJson(featuresJson)
        featureCollection.features()?.let {
            return it.toCollection(ArrayList())
        }
        return arrayListOf()
    }

    /** Creates a list of Features of given list of Handicap Objects */
    private fun createAccessibleZoneFeatures(responseBody: List<Handicap>?): ArrayList<Feature> {
        if (!responseBody.isNullOrEmpty()) {
            val features = ArrayList<Feature>()
            //for each Handicap object, create a feature and add it to a collection
            responseBody.forEach {zone ->
                val feature = Feature.fromGeometry(Point.fromLngLat(zone.long, zone.lat))
                feature.apply {
                    addStringProperty("id", zone.id)
                    addStringProperty("zone_name", zone.name)
                    addStringProperty("zone_owner", zone.owner)
                    addNumberProperty("parking_spaces", zone.parkingSpaces)
                    addStringProperty("max_parking_time", zone.maxParkingTime)
                    addStringProperty("max-parking_time_limitation", zone.maxParkingTimeLimitation)
                    addStringProperty("extra_info", zone.extraInfo)
                    addNumberProperty("distance", zone.distance)
                    addNumberProperty("lat", zone.lat)
                    addNumberProperty("long", zone.long)
                    addStringProperty("wkt", zone.WKT)
                }
                features.add(feature)
            }
            return features
        } else {
            return arrayListOf()
        }
    }

    override fun clearAccessibleZones() {
        this.accessibleZones.value = emptyList()
    }
}
