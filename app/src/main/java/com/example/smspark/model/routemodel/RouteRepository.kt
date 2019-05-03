package com.example.smspark.model.routemodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import org.koin.core.KoinComponent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RouteRepository(val context: Context): KoinComponent {

    val routeDestination: MutableLiveData<Point> by lazy {
        MutableLiveData<Point>()
    }

    val routeMap: MutableLiveData<HashMap<String, DirectionsRoute>> by lazy {
        MutableLiveData<HashMap<String, DirectionsRoute>>()
    }


    /** Returns a route from a start point, to a destination with a waypoint in between
     * @param start Start location of the route, usually the user location
     * @param parking A stop point in the route between start and destination
     * @param destination Final destination of the route*/
    fun getRoutes(start: Point, parking: Point, destination: Point, profile: String = "driving") {
        getRouteBuilder(start, parking, destination, profile).build().getRoute(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.body() == null) {
                    Log.e("RouteRepo", "No routes found")
                    return
                }
                if (routeMap.value.isNullOrEmpty()) {
                    //routeMap is empty, create a HashMap, put in the route and add it to the LiveData
                    val map = hashMapOf<String, DirectionsRoute>()
                    map[profile] = response.body()!!.routes()[0]
                    routeMap.value = map
                    //one route is fetched, fetch the second route recursivley
                    getRoutes(start, parking, destination, "walking")
                }
                routeMap.value?.let {
                    if (routeMap.value!!.size == 1) {
                        //there is one other route in the map, add the second one
                        it[profile] = response.body()!!.routes()[0]
                        routeMap.value = it
                        return
                    }
                    else {
                        //the map is full and is cleared, and then call this method recursivley to fetch the new routes
                        it.clear()
                        routeMap.value = it
                        getRoutes(start, parking, destination)
                    }
                }
            }
            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
            }
        })

    }


    private fun getRouteBuilder(start: Point, parking: Point, destination: Point, profile: String): NavigationRoute.Builder {
        val navigationRoute = NavigationRoute.builder(context)
                .accessToken(Mapbox.getAccessToken()!!)
                .profile(profile)
        if (profile == "walking") {
            navigationRoute.apply {
                origin(parking)
                destination(destination)
                addWaypointNames("Parkering", "Destination")
            }
        } else {
            navigationRoute.apply {
                origin(start)
                destination(parking)
                addWaypointNames("Start", "Parkering")
            }
        }
        return navigationRoute
    }
}