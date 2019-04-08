package com.example.smspark.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.example.smspark.model.Zone
import com.example.smspark.model.ZoneAdapter
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.chosen_zone.*
import kotlinx.android.synthetic.main.chosen_zone.view.*
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.round

@SuppressLint("LogNotTimber")
class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {

    // variables for adding location layer
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    private val REQUEST_CODE_AUTOCOMPLETE = 1

    // variables for adding location layer
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationComponent: LocationComponent

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

    private lateinit var snackbar: Snackbar
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val COLLAPSED = BottomSheetBehavior.STATE_COLLAPSED
    private val HIDDEN = BottomSheetBehavior.STATE_HIDDEN

    //lazy inject ViewModel
    val zoneViewModel: ZoneViewModel by sharedViewModel()
    val selectedZoneViewModel: SelectedZoneViewModel by viewModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireContext(), getString(R.string.access_token))
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity as MainActivity
        activity.changeNavBarVisibility(true)
        mapView = view.findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)
        initBottomSheet()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(getString(R.string.streets_parking)) { style ->
            enableLocationComponent(style)
            mapboxMap.addOnMapClickListener(this)
            setupImageSource(style)
            setupZoneLayers(style)
            setupMarkerLayer(style)
            initRecyclerView()
            initObservers()


        }
        initButtons()

        if(arguments != null){
            val fromPoint = Point.fromJson(arguments!!.getString("fromArg"))
            val destinationPoint = Point.fromJson(arguments!!.getString("destArg"))
            //zoneViewModel.getSpecificZones(destinationPoint.latitude(), destinationPoint.longitude(), 2000)

            zoneViewModel.getSpecificZones(destinationPoint.latitude(), destinationPoint.longitude(), 500).observe(this, Observer { data ->
                val gson = GsonBuilder().setLenient().create()
                //Filter out features that are polygons and points to seperate lists
                val polygonFeatures = data.features.filter { it.geometry.type == "Polygon" }
                val pointFeatures = data.features.filter { it.geometry.type == "Point" }

                val polygons = data.copy()
                val points = data.copy()
                polygons.features = polygonFeatures
                points.features = pointFeatures

                addPolygonsToMap(gson.toJson(polygons))
                addMarkersToMap(gson.toJson(points), false)

                val nearestFeature  = data.features.first()
                //TODO when the features is set and done convert and get closest parking and send it as waypoint
                getRoute(fromPoint, destinationPoint, destinationPoint)
            })
        }
    }

    /** Initiates ViewModel observers */
    private fun initObservers() {
        //ZoneViewModel observers

        //zoneViewModel.getHandicapZones().observe(this, Observer { data -> addMarkersToMap(data, true) })

        /*zoneViewModel.getSpecificZones(57.7089,11.9746,  500).observe(this, Observer { data ->

            val gson = GsonBuilder().setLenient().create()
            //Filter out features that are polygons and points to seperate lists
            val polygonFeatures = data.features.filter { it.geometry.type == "Polygon" }
            val pointFeatures = data.features.filter { it.geometry.type == "Point" }

            val polygons = data.copy()
            val points = data.copy()
            polygons.features = polygonFeatures
            points.features = pointFeatures

            addPolygonsToMap(gson.toJson(polygons))
            addMarkersToMap(gson.toJson(points), false)
        })*/

        /*zoneViewModel.zonePolygons.observe(this, Observer { polygons -> addPolygonsToMap(polygons) })
        zoneViewModel.zonePoints.observe(this, Observer { points -> addMarkersToMap(points,false) })
        zoneViewModel.handicapPoints.observe(this, Observer { handicapZones -> addMarkersToMap(handicapZones, true) })
        zoneViewModel.zoneFeatures.observe(this, Observer { features -> zoneAdapter.setData(features) })*/


        //SelectedZoneViewModel observers
        selectedZoneViewModel.selectedMapZone.observe(this, Observer { zone -> bottomSheetBehavior.state = COLLAPSED })
        selectedZoneViewModel.selectedListZone.observe(this, Observer { zone ->  bottomSheetBehavior.state = COLLAPSED})
    }

    /** Initiates button clickListeners */
    private fun initButtons() {
        fab_search.setOnClickListener {
            startAutoCompleteActivity()
        }
        my_locationFab.setOnClickListener {
            moveCameraToLocation()
        }
        sheet_ok_button.setOnClickListener {
            bottomSheetBehavior.state = HIDDEN
        }
        startNavigationButton!!.setOnClickListener {
            findNavController().navigate(R.id.mapFragment_to_navigation)
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

    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.state = HIDDEN
        val bottomSheetCallback = object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(newState == COLLAPSED) {
                    val selectedZone = selectedZoneViewModel.selectedListZone.value
                    bottomSheet.zoneName.text = selectedZone?.properties?.zoneName
                    bottomSheet.zoneCode.text = getString(R.string.zon_kod) + selectedZone?.properties?.zonecode.toString()
                    bottomSheet.zoneOwner.text = selectedZone?.properties?.zoneOwner
                    bottomSheet.zoneDistance.text = round(selectedZone!!.properties.distance).toString() + " m"
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        }
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap!!.locationComponent
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
                    .useDefaultLocationEngine(true)
                    .build())
            locationComponent.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING_GPS
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(requireActivity())
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapClick(point: LatLng): Boolean {
        val originPoint = getUserLocation()
        bottomSheetBehavior.state = HIDDEN
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
        val features = mapboxMap?.queryRenderedFeatures(pixel, pointLayer, polygonLayer, handicapLayer)
        if(features !=null && features.size > 0) {
            Log.d(TAG, "features queryed " + features.size)
            val feature = features[0]
            addMarkerOnMap(Point.fromLngLat(point.longitude, point.latitude), true)
            selectedZoneViewModel.selectedMapZone.value = feature
            return true
        }
        return false
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

    /** Adds frequently used GeoJson sources and layers to the map
     * @param loadedMapStyle The style to add sources and layers to*/
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

    /** Adds frequently used image sources to the map style
     * @param loadedMapStyle The style to add sources to*/
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
                            Log.e(TAG, "No routes found")
                            return
                        }
                        currentRoute = response.body()!!.routes()[0]
                        if(navigationMapRoute == null) {
                            navigationMapRoute = NavigationMapRoute(null, mapView!!, mapboxMap!!, R.style.NavigationMapRoute)
                        }
                        if (currentRoute != null) {
                            navigationMapRoute?.addRoute(currentRoute)
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
            navigationMapRoute?.updateRouteVisibilityTo(false)
            startNavigationButton.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            snackbar = Snackbar.make(coordinator, R.string.select_zone , Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            snackbarView.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext,R.color.mapbox_blue))
            snackbar.show()
        }
    }

    /** Handles the result given from the Search AutoComplete Activity*/
    private fun handleAutoCompleteResult(data: Intent?) {
        //Gets the place data from searched position
        val feature = PlaceAutocomplete.getPlace(data)
        feature?.let {
            destination = feature.geometry() as Point
            moveCameraToLocation(destination!!, 15.0, 2000)
            addMarkerOnMap(destination!!, false)
            if (currentRoute != null) {
                //if there is a previous route, reset it
                currentRoute = null
            }
        }
    }

    /** Called when an item in the RecyclerView is clicked
     * @param zone The list items binded object*/
    private fun zoneListItemClicked(zone: com.example.smspark.model.Feature) {
        if(zone != selectedZoneViewModel.selectedListZone.value) {
            bottomSheetBehavior.state = HIDDEN
            selectedZoneViewModel.selectedListZone.value = zone
            val wayPoint: Point
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
            if(destination != null) {
                getRoute(getUserLocation(), wayPoint, destination!!)
            }
        } else {
            Log.d(TAG, "Zone is equal to chosen zone")
        }
    }

    private fun moveCameraToLocation(point: Point = getUserLocation(), tilt: Double = 0.0, duration: Int = 2000) {
        mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(point.latitude(), point.longitude()))
                .zoom(14.0)
                .tilt(tilt)
                .build()), duration)
    }


    @SuppressLint("MissingPermission")
    private fun getUserLocation(): Point = Point.fromLngLat(locationComponent.lastKnownLocation!!.longitude, locationComponent.lastKnownLocation!!.latitude)


    private fun getMapStyle(): Style = mapboxMap?.style!!

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(requireContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(getMapStyle())
        } else {
            Toast.makeText(requireContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
    }

    private fun toggleListVisibility() {
        when(recyclerView.visibility) {
            View.VISIBLE -> recyclerView.visibility = View.GONE
            View.GONE -> recyclerView.visibility = View.VISIBLE
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
