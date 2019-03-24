package com.example.smspark.model

import retrofit2.Call
import retrofit2.http.GET

interface ZoneService {

    @GET("features/search?app_code=38cd95124d0c4c4045550a90664ec77a&lat=57.7088&lon=11.9745&distance=2000")
    fun getZones() : Call<Zone>
}