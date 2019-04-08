package com.example.smspark.dimodules


import com.example.smspark.model.ZoneModel.ZoneService
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


val BASE_SMSPARK_URL = "https://api.smspark.se/rest/v2/"
val BASE_GBGSTAD_URL = "TBD"

val webserviceModule  = module{
    single {createRetrofitService<ZoneService>()}
}
inline fun <reified T> createRetrofitService() : T {
    val retrofit = Retrofit.Builder()
            .baseUrl(BASE_SMSPARK_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    return retrofit.create((T::class.java))
}