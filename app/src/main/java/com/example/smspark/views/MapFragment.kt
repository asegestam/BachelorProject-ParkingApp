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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.example.smspark.model.ZoneAdapter
import com.example.smspark.viewmodels.ZoneViewModel
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.plugins.annotation.Fill
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder

@SuppressLint("LogNotTimber")
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
    private var destination: Point? = null

    //RecyclerView fields
    private lateinit var recyclerView: RecyclerView
    private lateinit var zoneAdapter: ZoneAdapter
    //Layer and Source Strings
    //Polygon
    private val polygonLayer = "zone-polygons-layer"
    private val polygonSource = "polygon-source"
    //Point
    private val pointLayer = "zone-point-layer"
    private val pointSource = "point-source"
    //Handicap
    private val handicapLayer = "handicap-layer"
    private val handicapSource = "handicap-source"
    //Marker
    private val markerSource = "marker-layer"
    //Images
    private val markerImage = "marker-image"
    private val parkingImage = "parking-image"
    private val handicapImage = "handicap-image"

    //lazy inject ViewModel
    val zoneViewModel: ZoneViewModel by sharedViewModel()



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
            setupImageSource(style)
            setupZoneLayers(style)
            setupMarkerLayer(style)
            zoneViewModel.getZones()
            zoneViewModel.getHandicapZones()
            initObservers()
            initRecyclerView()
        }
        initButtons()
    }

    /** Initiates ViewModel observers */
    private fun initObservers() {
        zoneViewModel.zonePolygons.observe(this, Observer { polygons -> addPolygonsToMap(polygons) })
        zoneViewModel.zoneFeatures.observe(this, Observer { features -> zoneAdapter.setData(features) })
        zoneViewModel.zoneChosen.observe(this, Observer { feature ->  })
        zoneViewModel.zonePoints.observe(this, Observer { points ->
            addMarkersToMap(points,false)
        })
        zoneViewModel.handicapPoints.observe(this, Observer { handicapZones ->
            addMarkersToMap(handicapZones, true)
        })

    }

    /** Initiates button clickListeners */
    private fun initButtons() {
        fab.setOnClickListener {
            startAutoCompleteActivity()
        }
        startNavigationButton!!.setOnClickListener {
            startNavigationUI()
        }
        list_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                recycler_view.visibility = View.VISIBLE
            } else {
                recycler_view.visibility = View.GONE
            }
        }
    }


    /**Initiates the RecyclerView with a adapter, clickListener, LayoutManager, Animator, SnapHelper*/
    private fun initRecyclerView() {
        recyclerView = recycler_view
        recyclerView.setHasFixedSize(true)
        zoneAdapter = ZoneAdapter(context!!) { zone: com.example.smspark.model.Feature -> zoneListItemClicked(zone)}

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = zoneAdapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
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
            permissionsManager?.requestLocationPermissions(requireActivity())
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

    /** Queryes the map for zone features on the point clicked
     *
     * @param point Location to query
     * */
    private fun queryMapClick(point: LatLng): Boolean {
        val pixel = mapboxMap?.projection!!.toScreenLocation(point)
        val features = mapboxMap?.queryRenderedFeatures(pixel)
        features?.let {
            Log.d(TAG, "" + features.size)
            for (feature in features) {
                //Only relevant features has zonecodes
                when {
                    feature.hasProperty("zonecode") -> handleChosenZone(feature, Point.fromLngLat(point.longitude, point.latitude), false)
                    feature.hasProperty("Owner") -> handleChosenZone(feature, Point.fromLngLat(point.longitude, point.latitude), true)
                }
                return true
            }
        }
        return false
    }

    private fun handleChosenZone(feature: Feature?, point: Point?, b: Boolean) {
        if(b) {
            Toast.makeText(requireContext(), "Vald Zon: " + feature?.getProperty("Name"), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "Vald Zon: " + feature?.getProperty("zone_name"), Toast.LENGTH_LONG).show()
        }
        point?.let { addMarkerOnMap(point, true) }
    }

    /** Adds a marker layer to be reused over and over
     * @param loadedMapStyle Mapbox style to add Layers and Sources to
     * */
    private fun setupMarkerLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(markerSource))
        loadedMapStyle.addLayer(SymbolLayer("marker-layer", markerSource)
                .withProperties(PropertyFactory.iconImage(markerImage),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)))
    }

    /** Sets an marker on the given point
     * @param point Where the marker is getting set
     * @param isWayPoint if the SymbolLayer should have 2 markers or 1
     * */
    private fun addMarkerOnMap(point: Point, isWayPoint: Boolean) {
        val source = mapboxMap?.style!!.getSourceAs<GeoJsonSource>(markerSource)
        if(source != null) {
            if(isWayPoint) {
                source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(destination), Feature.fromGeometry(point))))
            } else {
                source.setGeoJson(destination)
            }
        }
    }

    private fun setupZoneLayers(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(polygonSource))
        loadedMapStyle.addSource(GeoJsonSource(pointSource))
        loadedMapStyle.addSource(GeoJsonSource(handicapSource))
        //Polygon Layer
        loadedMapStyle.addLayer(FillLayer(polygonLayer, polygonSource)
                .withProperties(
                        PropertyFactory.fillColor(Color.parseColor("#f42428")),
                        PropertyFactory.fillOpacity(0.75f)))
        //Point Layer
        loadedMapStyle.addLayer(SymbolLayer(pointLayer, pointSource)
                .withProperties(PropertyFactory.iconImage(parkingImage), iconSize(0.35f)))
        //Handicap Layer
        loadedMapStyle.addLayer(SymbolLayer(handicapLayer, handicapSource)
                .withProperties(PropertyFactory.iconImage(handicapImage), iconSize(0.8f)))

    }
    private fun setupImageSource(loadedMapStyle: Style) {
        loadedMapStyle.addImage(markerImage, BitmapFactory.decodeResource(resources, R.drawable.mapbox_marker_icon_default))
        loadedMapStyle.addImage(parkingImage, BitmapFactory.decodeResource(resources, R.drawable.park_blue))
        loadedMapStyle.addImage(handicapImage, BitmapFactory.decodeResource(resources, R.drawable.handicap_icon))
    }

    /** Adds a FillLayer representation of a given JSON String
     * @param json Valid JSON string containing Polygon Features */
    private fun addPolygonsToMap(json: String) {
        val source = getMapStyle().getSourceAs<GeoJsonSource>(polygonSource)
        source?.setGeoJson(json)
    }

    /** Adds a SymbolLayer representation of a given JSON String, where the icon is a Parking Icon
     * @param json Valid JSON string containing Point Features
     * @param isHandicap indicates if the given JSON is handicap zones, used to change marker icon*/
    private fun addMarkersToMap(json: String, isHandicap: Boolean) {
        if(isHandicap) {
            val handicapSource = getMapStyle().getSourceAs<GeoJsonSource>(handicapSource)
            handicapSource?.setGeoJson(json)
        } else {
            val pointSource = getMapStyle().getSourceAs<GeoJsonSource>(pointSource)
            pointSource?.setGeoJson(json)
        }
    }

    /** Returns a route from a origin point, to a destination with a waypoint in between
     * @param origin Start location of the route, usually the user location
     * @param wayPoint A stop point in the route between start and destination
     * @param destination Final destination of the route*/
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

            Toast.makeText(requireContext(), "Please select a zone", Toast.LENGTH_SHORT).show()
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
                .tilt(15.0)
                .build()), 2000)
        addMarkerOnMap(destination!!, false)
        if (currentRoute != null) {
            //if there is a previous route, reset it
            currentRoute = null
        }
    }

    /** Called when an item in the RecyclerView is clicked
     * @param zone The list items binded object*/
    private fun zoneListItemClicked(zone: com.example.smspark.model.Feature) {
        if(!zone.equals(zoneViewModel.zoneChosen.value)) {
            zoneViewModel.zoneChosen.value = zone
            var wayPoint: Point? = null
            if (zone.geometry.type == "Point") {
                val pointCoordinates = zone.geometry.coordinates as List<Double>
                wayPoint = Point.fromLngLat(pointCoordinates.get(0), pointCoordinates.get(1))
                addMarkerOnMap(wayPoint, true)
            } else {
                val polygonCoordinates = zone.geometry.coordinates as List<List<List<Double>>>
                val long = polygonCoordinates[0][0][0]
                val lat = polygonCoordinates[0][0][1]
                wayPoint = Point.fromLngLat(long, lat)
                addMarkerOnMap(wayPoint, true)
            }
            Toast.makeText(requireContext(), "Vald Zon: " + zone.properties.zonecode, Toast.LENGTH_LONG).show()
            getRoute(getUserLocation(), wayPoint, destination!!)
        } else {
            Log.d(TAG, "Zone is equal to chosen zone")
        }

    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(): Point = Point.fromLngLat(locationComponent!!.lastKnownLocation!!.longitude, locationComponent!!.lastKnownLocation!!.latitude)

    private fun getMapStyle(): Style = mapboxMap?.style!!

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

    /**  ------ LifeCycle Methods ------*/
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

    override fun onDestroyView() {
        super.onDestroyView()
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
