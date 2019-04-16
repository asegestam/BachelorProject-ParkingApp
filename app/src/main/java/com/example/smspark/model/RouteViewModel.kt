package com.example.smspark.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class RouteViewModel(val context: Context): ViewModel() {

    val route: MutableLiveData<DirectionsRoute> by lazy {
        MutableLiveData<DirectionsRoute>()
    }

    /** Returns a route from a origin point, to a destination with a waypoint in between
     * @param origin Start location of the route, usually the user location
     * @param destination Final destination of the route*/
    fun getSimpleRoute(origin: Point, destination: Point, profile: String) {
        NavigationRoute.builder(context)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin)
                .profile(profile)
                .destination(destination)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        if (response.body() == null) {
                            Timber.e("No routes found")
                            return
                        }
                        route.value = response.body()!!.routes()[0]
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                    }
                })

    }
    /** Returns a route from a origin point, to a destination with a waypoint in between
     * @param origin Start location of the route, usually the user location
     * @param wayPoint A stop point in the route between start and destination
     * @param destination Final destination of the route*/
    fun getWayPointRoute(origin: Point?, wayPoint: Point, destination: Point, profile: String) {
        if (origin != null) {
            NavigationRoute.builder(context)
                    .accessToken(Mapbox.getAccessToken()!!)
                    .origin(origin)
                    .addWaypoint(wayPoint)
                    .profile(profile)
                    .destination(destination)
                    .addWaypointNames("Start", "Parkering", "Destination")
                    .build()
                    .getRoute(object : Callback<DirectionsResponse> {
                        override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                            if (response.body() == null) {
                                Timber.e("No routes found")
                                return
                            }
                            route.value = response.body()!!.routes()[0]
                            Log.d("RouteViewModel", route.value.toString())
                        }

                        override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                        }
                    })

        }
    }
}