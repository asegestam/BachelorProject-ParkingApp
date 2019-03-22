package com.example.smspark.model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import org.koin.core.KoinComponent
import org.koin.core.inject
import retrofit2.Call
import retrofit2.Response

class ZoneRepositoryImpl: ZoneRepository, KoinComponent {

    private val TAG  = "ZoneRepositoryImpl"
    private val service: ZoneService by inject()

    override fun getZones(): MutableLiveData<String> {
        val data = MutableLiveData<String>()
        val call = service.getZones()
        call?.enqueue(object : retrofit2.Callback<Zone> {
            override fun onFailure(call: Call<Zone>, t: Throwable) {
                Log.e(TAG, t.message)
            }

            override fun onResponse(call: Call<Zone>, response: Response<Zone>) {
                val parking = response.body()
                val gson = GsonBuilder().setPrettyPrinting().create()
                val polygons = parking?.features?.toCollection(ArrayList())?.filter {  it.geometry.type == "Polygon" }
                val polygonsString: String = "{\"type\": \"FeatureCollection\",\"features\":" + gson.toJson(polygons) + "}"
                println(polygonsString)
                data.value = gson.toJson(polygons)

            }

        })
        return data
    }

    override fun getZone(): String = "Zone 666"


}