package com.example.smspark.views


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.example.smspark.R
import com.example.smspark.viewmodels.ZoneViewModel
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
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.android.viewmodel.ext.android.viewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder



class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {

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
    private var goteborg: Point = Point.fromLngLat(11.9745, 57.7088)
    private var destination: Point? = null

    //lazy inject ViewModel
    val zoneViewModel: ZoneViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(requireContext(), getString(R.string.access_token))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        mapView = view?.findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(getString(R.string.streets_parking)) { style ->
            enableLocationComponent(style)
            mapboxMap.addOnMapClickListener(this)

            zoneViewModel.zonePolygons.observe(this, Observer { polygons -> addPolygonsToMap(polygons) })
            zoneViewModel.zonePoints.observe(this, Observer { points -> addMarkersToMap(points) })
            zoneViewModel.handicapPoints.observe(this, Observer { handicapZones -> addHandicapMarkerToMap(handicapZones) })
            zoneViewModel.getZones()
            zoneViewModel.getHandicapZones()
        }
        initButtons()

    }

    private fun initButtons() {
        fab.setOnClickListener {
            startAutoCompleteActivity()
        }
        startNavigationButton!!.setOnClickListener {
            startNavigationUI()
        }

        profile_btn.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_mapFragment_to_profileFragment))
        tickets_btn.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_mapFragment_to_ticketsFragment))
        trip_btn.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_mapFragment_to_tripFragment))
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap!!.locationComponent
            locationComponent?.activateLocationComponent(requireContext(), loadedMapStyle)
            locationComponent?.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent?.cameraMode = CameraMode.TRACKING
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(requireActivity())
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapClick(point: LatLng): Boolean {
        val originPoint = getUserLocation()
        if (destination != null && queryMapClick(point)) {
            val wayPoint = Point.fromLngLat(point.longitude, point.latitude)
            val source = mapboxMap?.style!!.getSourceAs<GeoJsonSource>("map-click-marker")
            source?.setGeoJson(Feature.fromGeometry(wayPoint))
            getRoute(originPoint, wayPoint, destination!!)
        }
        return true
    }

    /** Queryes the map for zone features on the point clicked */
    private fun queryMapClick(point: LatLng) : Boolean{
        val pixel = mapboxMap?.projection!!.toScreenLocation(point)
        val features = mapboxMap?.queryRenderedFeatures(pixel)
        features?.let {
            Log.d(TAG, "" + features.size)
            for(feature in features) {
                //Only relevant features has zonecodes
                if(feature.hasProperty("zonecode") || feature.hasProperty("Owner")) {
                    val stringBuilder = StringBuilder()
                    for((key, value) in feature.properties()!!.entrySet()) {
                        Log.d(TAG, String.format("%s = %s", key, value))
                        stringBuilder.append(value)
                        stringBuilder.append("\n")
                    }
                    Toast.makeText(requireContext(), stringBuilder.toString(),  Toast.LENGTH_LONG).show()
                    return true
                }
            }
        }
        return false
    }

    /** Returns a route from a origin point, to a destination with a waypoint in between */
    private fun getRoute(origin: Point, wayPoint: Point, destination: Point) {
        NavigationRoute.builder(requireContext())
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin)
                .addWaypoint(wayPoint)
                .profile("driving")
                .destination(destination)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.")
                            return
                        } else if (response.body()!!.routes().size < 1) {
                            Log.e(TAG, "No routes found")
                            return
                        }
                        currentRoute = response.body()!!.routes()[0]
                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            //navigationMapRoute!!.removeRoute()
                        } else {
                            navigationMapRoute = NavigationMapRoute(null, mapView!!, mapboxMap!!, R.style.NavigationMapRoute)
                        }
                        if (currentRoute != null) {
                            navigationMapRoute!!.addRoute(currentRoute)
                            startNavigationButton.visibility = View.VISIBLE
                        } else {
                            Log.e(TAG, "Error, route is null")
                        }
                    }
                    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                        Log.e(TAG, "Error: " + throwable.message)
                    }
                })
    }

    /** Starts the navigationUI to navigate to the currentRoute */
    private fun startNavigationUI() {
        val simulateRoute = true
        val options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(simulateRoute)
                .build()
        // Call this method with Context from within an Activity
        NavigationLauncher.startNavigation(requireActivity(), options)
    }

    /** Starts a Search AutoComplete activity for searching locations */
    private fun startAutoCompleteActivity() {
        val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(getString(R.string.access_token))
                .placeOptions(PlaceOptions.builder()
                        .language("sv")
                        .country("SE")
                        .proximity(getUserLocation())
                        .build(PlaceOptions.MODE_CARDS))
                .build(requireActivity())
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //if result code is for AutoComplete activity
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            handleAutoCompleteResult(data)

            zoneViewModel.getZones()
            zoneViewModel.getHandicapZones()

            navigationMapRoute?.updateRouteVisibilityTo(false)
            startNavigationButton.visibility = View.GONE

            Toast.makeText(requireContext(), "Please select a zone" , Toast.LENGTH_SHORT).show()
        }
    }

    /** Handles the result given from the Search AutoComplete Activity*/
    private fun handleAutoCompleteResult(data: Intent?) {
        //Gets the place data from searched position
        val feature = PlaceAutocomplete.getPlace(data)
        destination = feature?.geometry() as Point
        val latLng = LatLng(destination!!.latitude(), destination!!.longitude())
        //Animates the camera to the searched position
        mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(latLng)
                .zoom(14.0)
                .bearing(90.0)
                .tilt(15.0)
                .build()), 2000)
        val source = mapboxMap?.style!!.getSourceAs<GeoJsonSource>("destination-map-marker")
        source?.setGeoJson(Feature.fromGeometry(destination))
        if(currentRoute != null) {
            //if there is a previous route, reset it
            currentRoute = null
        }
    }

    /** Adds a FillLayer representation of a given JSON String */
    private fun addPolygonsToMap(json: String) {
        /*
       Mapbox does not allow us to edit current layers, so before adding a layer
       we remove the existing one if it exists
        */
        val layer = getMapStyle().getLayer("zonePolygonsLayer")
        val source = getMapStyle().getSource("zonePolygons")
        if(layer != null) { getMapStyle().removeLayer(layer)}
        if(source != null) { getMapStyle().removeSource(source)}

        getMapStyle().addSource(GeoJsonSource("zonePolygons", json))
        val zonePolygonsLayer = FillLayer("zonePolygonsLayer", "zonePolygons")
        zonePolygonsLayer.setProperties(PropertyFactory.fillColor(Color.parseColor("#f42428")),
                PropertyFactory.fillOpacity(0.75f))
        getMapStyle().addLayerAbove(zonePolygonsLayer, "road-rail-tracks")
    }

    /** Adds a SymbolLayer representation of a given JSON String, where the icon is a Parking Icon */
    private fun addMarkersToMap(json: String) {
        /*
        Mapbox does not allow us to edit current layers, so before adding a layer
        we remove the existing one if it exists
         */
        val layer = getMapStyle().getLayer("zoneMarkerLayer")
        val source = getMapStyle().getSource("zonePoint")
        if(layer != null) { getMapStyle().removeLayer(layer)}
        if(source != null) { getMapStyle().removeSource(source)}

        getMapStyle().addSource(GeoJsonSource("zonePoint", json))
        getMapStyle().addImage("parking_marker", BitmapFactory.decodeResource(resources, R.drawable.park_blue))
        getMapStyle().addLayer(SymbolLayer("zoneMarkerLayer", "zonePoint")
                .withProperties(PropertyFactory.iconImage("parking_marker"), PropertyFactory.iconSize(0.35f)))
    }

    /** Adds a SymbolLayer representation of a given JSON String, where the icon is a Parking Icon */
    private fun addHandicapMarkerToMap(json: String) {
        /*
        Mapbox does not allow us to edit current layers, so before adding a layer
        we remove the existing one if it exists
         */
        val layer = getMapStyle().getLayer("handicapZoneLayer")
        val source = getMapStyle().getSource("handicapZonePoint")
        if(layer != null) { getMapStyle().removeLayer(layer)}
        if(source != null) { getMapStyle().removeSource(source)}

        getMapStyle().addSource(GeoJsonSource("handicapZonePoint", json))
        getMapStyle().addImage("handicap_marker", BitmapFactory.decodeResource(resources, R.drawable.handicap_icon))
        getMapStyle().addLayer(SymbolLayer("handicapZoneLayer", "handicapZonePoint")
                .withProperties(PropertyFactory.iconImage("handicap_marker"), PropertyFactory.iconSize(0.8f)))
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() : Point = Point.fromLngLat(locationComponent!!.lastKnownLocation!!.longitude, locationComponent!!.lastKnownLocation!!.latitude)

    private fun getMapStyle() : Style = mapboxMap?.style!!

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(requireContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap!!.style!!)
        } else {
            Toast.makeText(requireContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
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
        private val TAG = "MapFragment"
    }
}
