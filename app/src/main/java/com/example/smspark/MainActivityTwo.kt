package com.example.smspark

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions.MODE_CARDS
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.activity_main_two.*


class MainActivityTwo : AppCompatActivity(), PermissionsListener, OnMapReadyCallback, MapboxMap.OnMapClickListener {

    private val TAG: String = "MainActivityTwo"
    private val REQUEST_CODE_AUTOCOMPLETE = 1
    private var permissionsManager : PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap : MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        setContentView(R.layout.activity_main_two)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            style -> enableLocationComponent()
        }
        val markerViewManager = MarkerViewManager(mapView, mapboxMap)
        initFab()
    }

    private fun initFab() {
        fab.setOnClickListener {
            val intent = PlaceAutocomplete.IntentBuilder()
                    .accessToken(getString(R.string.access_token))
                    .placeOptions(PlaceOptions.builder()
                            .language("sv")
                            .country("SE")
                            .build(MODE_CARDS))

                    .build(this)
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            //Gets the place data from searched position
            val feature = PlaceAutocomplete.getPlace(data)
            val latLng: LatLng = LatLng((feature.geometry() as com.mapbox.geojson.Point).latitude(),
                    (feature.geometry() as com.mapbox.geojson.Point).longitude())
            //Animates the camera to the searched position
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                    .target(latLng)
                    .zoom(14.0)
                    .bearing(90.0)
                    .tilt(15.0)
                    .build()), 4000)
            mapboxMap.clear()
            mapboxMap.addMarker(com.mapbox.mapboxsdk.annotations.MarkerOptions().position(latLng))
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            val options = LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
                    .accuracyColor(ContextCompat.getColor(this, R.color.mapbox_blue))
                    .build()

            // Get an instance of the component
            val locationComponent = mapboxMap.locationComponent

            // Activate the component
            locationComponent.activateLocationComponent(this, mapboxMap?.style!!)


            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }


    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent()
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        finishActivity(REQUEST_CODE_AUTOCOMPLETE)
        return true
    }


    //Lifecycle methods below
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
