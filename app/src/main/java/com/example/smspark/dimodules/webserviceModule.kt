package com.example.smspark.dimodules


import com.example.smspark.model.zonemodel.ZoneService
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


const val BASE_SMSPARK_URL = "https://api.smspark.se/rest/v2/features/"

val webserviceModule  = module{
    single {createRetrofitService<ZoneService>()}
}
inline fun <reified T> createRetrofitService() : T {
    val retrofit = Retrofit.Builder()
            .baseUrl(BASE_SMSPARK_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    return retrofit.create((T::class.java))
}