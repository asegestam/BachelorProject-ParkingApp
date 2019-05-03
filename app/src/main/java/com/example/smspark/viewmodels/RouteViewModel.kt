package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.routemodel.RouteRepository
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point

class RouteViewModel(private val repo: RouteRepository) : ViewModel() {

     val routeDestination: MutableLiveData<DirectionsRoute> by lazy {
        MutableLiveData<DirectionsRoute>()
    }

    val routeWayPoint: MutableLiveData<DirectionsRoute> by lazy {
        MutableLiveData<DirectionsRoute>()
    }

    val destination: MutableLiveData<Point> = repo.routeDestination

    val routeMap: MutableLiveData<HashMap<String, DirectionsRoute>> = repo.routeMap


    fun getRoutes(): MutableLiveData<HashMap<String, DirectionsRoute>> {
        return repo.routeMap
    }

    fun getWayPointRoute(origin: Point, wayPoint: Point, destination: Point) {
        return repo.getRoutes(origin, wayPoint, destination)
    }

}
