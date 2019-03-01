package com.example.smspark

import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions.MODE_CARDS
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener, ProgressChangeListener {

    // variables for adding location layer
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    private val REQUEST_CODE_AUTOCOMPLETE = 1

    // variables for adding location layer
    private var permissionsManager: PermissionsManager? = null
    private var locationComponent: LocationComponent? = null

    // variables for calculating and drawing a route
    private var currentRoute: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var sjukhus: Point = Point.fromLngLat(57.661244, 12.012948)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day)) { style ->
            enableLocationComponent(style)
            addDestinationIconSymbolLayer(style)
            mapboxMap.addOnMapClickListener(this@MainActivity)

            startNavigationButton!!.setOnClickListener {
                Log.d(TAG, "onClick: Trying to start the simulation of the navigation")
                val simulateRoute = true
                val options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(simulateRoute)
                        .build()
                // Call this method with Context from within an Activity
                NavigationLauncher.startNavigation(this@MainActivity, options)
            }
            initFab()
        }
    }

    private fun addDestinationIconSymbolLayer(loadedMapStyle: Style) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default))
        val geoJsonSource = GeoJsonSource("destination-source-id")
        loadedMapStyle.addSource(geoJsonSource)
        val destinationSymbolLayer = SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        )
        loadedMapStyle.addLayer(destinationSymbolLayer)
    }

    @SuppressLint("MissingPermission")
    override fun onMapClick(point: LatLng): Boolean {
/*
        val destinationPoint = Point.fromLngLat(point.longitude, point.latitude)
        val originPoint = Point.fromLngLat(locationComponent!!.lastKnownLocation!!.longitude,
                locationComponent!!.lastKnownLocation!!.latitude)

        val source = mapboxMap!!.style!!.getSourceAs<GeoJsonSource>("destination-source-id")
        source?.setGeoJson(Feature.fromGeometry(destinationPoint))

        //getRoute(originPoint, destinationPoint)
        */
        return true
    }

    private fun getRoute(origin: Point, destination: Point) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin)
                .profile("driving")
                .destination(destination)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code())
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.")
                            return
                        } else if (response.body()!!.routes().size < 1) {
                            Log.e(TAG, "No routes found")
                            return
                        }

                        Log.d(TAG, "onResponse: Number of routes: " + response.body()!!.routes().size)

                        currentRoute = response.body()!!.routes()[0]

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute!!.removeRoute()
                        } else {
                            navigationMapRoute = NavigationMapRoute(null, mapView!!, mapboxMap!!, R.style.NavigationMapRoute)
                        }
                        if (currentRoute != null) {
                            navigationMapRoute!!.addRoute(currentRoute)
                            startNavigationButton!!.isEnabled = true
                            startNavigationButton!!.setBackgroundResource(R.color.mapbox_blue)
                        } else {
                            Log.e(TAG, "Error, route is null")
                        }
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                        Log.e(TAG, "Error: " + throwable.message)
                    }
                })
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap!!.locationComponent
            locationComponent!!.activateLocationComponent(this, loadedMapStyle)
            locationComponent!!.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent!!.cameraMode = CameraMode.TRACKING
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    private fun initFab() {
        fab.setOnClickListener {
            val intent = PlaceAutocomplete.IntentBuilder()
                    .accessToken(getString(R.string.access_token))
                    .placeOptions(PlaceOptions.builder()
                            .language("sv")
                            .country("SE")
                            .proximity(getUserLocation())
                            .build(MODE_CARDS))

                    .build(this)
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            //Gets the place data from searched position
            val feature = PlaceAutocomplete.getPlace(data)
            val destination: Point = feature.geometry() as Point
            val latLng = LatLng(destination.latitude(), destination.longitude())
            //get and display the route to searched location
            getRoute(getUserLocation(), destination)
            //Animates the camera to the searched position
            mapboxMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                    .target(latLng)
                    .zoom(14.0)
                    .bearing(90.0)
                    .tilt(15.0)
                    .build()), 4000)
            mapboxMap!!.clear()
            mapboxMap!!.addMarker(com.mapbox.mapboxsdk.annotations.MarkerOptions().position(latLng))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap!!.style!!)
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() : Point {
        return Point.fromLngLat(locationComponent!!.lastKnownLocation!!.longitude,
                locationComponent!!.lastKnownLocation!!.latitude)
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        Log.i(TAG, "route progress changed")
        if(routeProgress!!.currentState()!! == RouteProgressState.ROUTE_ARRIVED)
            Toast.makeText(applicationContext, "Route finished", Toast.LENGTH_SHORT).show()
    }


    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    companion object {
        private val TAG = "DirectionsActivity"
    }
}
