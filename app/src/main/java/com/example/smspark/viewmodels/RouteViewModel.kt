package com.example.smspark.viewmodels

import androidx.lifecycle.LiveData
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


    fun getRoute(): LiveData<DirectionsRoute> {
        return repo.route
    }

    fun getSimpleRoute(origin: Point, destination: Point, profile: String) {
        return repo.getSimpleRoute(origin, destination, profile)
    }

    fun getWayPointRoute(origin: Point, wayPoint: Point, destination: Point) {
        return repo.getWayPointRoute(origin, wayPoint, destination)
    }

}
