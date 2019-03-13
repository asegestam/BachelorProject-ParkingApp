package com.example.smspark

import com.example.smspark.dto.Parking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClientInstance {

    private var retrofit: Retrofit? = null

    private val BASE_URL = "https://api.smspark.se/rest/v2/"

    val retrofitInstance: Retrofit?
    get() {
        if (retrofit == null){
            retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
        }
        return retrofit
    }

}