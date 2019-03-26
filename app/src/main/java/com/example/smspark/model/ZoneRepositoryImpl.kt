package com.example.smspark.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import org.koin.core.KoinComponent
import org.koin.core.inject
import retrofit2.Call
import retrofit2.Response

class ZoneRepositoryImpl: ZoneRepository, KoinComponent {

    private val TAG  = "ZoneRepositoryImpl"
    private val service: ZoneService by inject()
    val data = MutableLiveData<HashMap<String, String>>()
    val handicap = MutableLiveData<String>()

    override fun getZones(): LiveData<HashMap<String, String>> {
        val call = service.getZones()
        call.enqueue(object : retrofit2.Callback<Zone> {
            override fun onFailure(call: Call<Zone>, t: Throwable) {
                Log.e(TAG, t.message)
            }

            override fun onResponse(call: Call<Zone>, response: Response<Zone>) {
                if(response.isSuccessful) {
                    val zones = response.body()!!
                    val gson = GsonBuilder().setLenient().create()
                    //Filter out features that are polygons and points to seperate lists
                    val polygonFeatures = zones.features.toCollection(ArrayList()).filter { it.geometry.type == "Polygon" }
                    val pointFeatures = zones.features.toCollection(ArrayList()).filter { it.geometry.type == "Point" }

                    val polygons = zones.copy()
                    val points = zones.copy()
                    polygons.features = polygonFeatures
                    points.features = pointFeatures

                    println("polygon filtered zone object to json " + gson.toJson(zones))
                    data.value = hashMapOf("polygons" to gson.toJson(polygons), "points" to gson.toJson(points))
                }
            }
        })
        return data
    }

    override fun getHandicapZones(): MutableLiveData<String> {
        val call = service.getHandicapZones()
        call.enqueue(object : retrofit2.Callback<List<Handicap>> {

            override fun onFailure(call: Call<List<Handicap>>, t: Throwable) {
                Log.e(TAG, t.message)
            }

            override fun onResponse(call: Call<List<Handicap>>, response: Response<List<Handicap>>) {
                if(response.isSuccessful) {
                    val zones = response.body()!!
                    var geojsonString = "{\"type\":\"FeatureCollection\"," +
                                                "\"features\":["

                    var listIterator = zones.listIterator()

                    while (listIterator.hasNext()) {
                        var handicap = listIterator.next()
                        geojsonString +=
                                "{" +
                                    "\"type\":\"Feature\"," +
                                    "\"geometry\":{" +
                                        "\"type\":\"Point\"," +
                                        "\"coordinates\":[" + "${handicap.long}," + "${handicap.lat}" + "]" +
                                        "}," +
                                    "\"properties\":{" +
                                            "\"Id\":\"${handicap.id}\"," +
                                            "\"Name\":\"${handicap.name}\"," +
                                            "\"Owner\":\"${handicap.owner}\"," +
                                            "\"ParkingSpaces\":${handicap.parkingSpaces}," +
                                            "\"MaxParkingTime\":\"${handicap.maxParkingTime}\"," +
                                            "\"Distance\":${handicap.distance}," +
                                            "\"WKT\":\"${handicap.WKT}\"" +
                                        "}" +
                                "}"

                        if (listIterator.hasNext())
                            geojsonString += ","
                    }

                    geojsonString += "]}"

                    println("Handicap call to GBGSTAD parsed: " + geojsonString)
                    handicap.value = geojsonString
                }
            }
        })

        return handicap
    }

}