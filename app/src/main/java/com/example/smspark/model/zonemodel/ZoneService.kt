package com.example.smspark.model.zonemodel


import kotlinx.coroutines.Deferred
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ZoneService {

    @GET("https://data.goteborg.se/ParkingService/v2.1/HandicapParkings/1d6e729b-4f57-4c48-be1b-712bac46a08e?&format=JSON")
    fun getHandicapZones(@Query("latitude") latitude: Double,
                         @Query("longitude") longitude: Double,
                         @Query("radius") radius: Int) : Deferred<Response<List<Handicap>>>

    @GET("search?app_code=38cd95124d0c4c4045550a90664ec77a")
    fun getSpecificZones(@Query("lat") latitude: Double,
                         @Query("lon") longitude: Double,
                         @Query("distance") radius: Int) : Deferred<Response<Zone>>
}