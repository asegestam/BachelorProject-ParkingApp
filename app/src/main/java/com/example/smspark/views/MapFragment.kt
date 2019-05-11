package com.example.smspark.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
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
import com.example.smspark.model.GeometryUtils
import com.example.smspark.model.observeOnce
import com.example.smspark.model.changeValue
import com.example.smspark.viewmodels.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.sources.Source
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.selected_zone.*
import kotlinx.android.synthetic.main.selected_zone.view.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber


class MapFragment : Fragment(), MapboxMap.OnMapClickListener, MapboxMap.OnMapLongClickListener , MapboxMap.OnMoveListener {

    private val mainActivity: MainActivity by lazy { activity as MainActivity }
    // variables for adding location layer
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private val requestCodeAutoComplete = 1
    // variables for calculating and drawing a route
    private val navigationMapRoute by lazy {
        NavigationMapRoute(null, mapView, mapboxMap!!, R.style.NavigationMapRoute)
    }
    //RecyclerView fields
    private lateinit var recyclerView: RecyclerView
    private lateinit var zoneAdapter: ZoneAdapter
    //Layer and Source Strings
    //Polygon
    private val polygonLayerID = "zone-polygons-layer"
    private val polygonSourceID = "polygon-source"
    private val polygonHighlightID = "polygon-highlight"
    //Sources and layers relating to highlighting selected zone
    private val selectedZoneLayerID = "selected-zone-layer"
    private val selectedZoneSourceID = "selected-zone-source"
    private val selectedZoneHighLightID = "highlight-zone-layer"
    //Point
    private val pointLayerID = "zone-point-layer"
    private val pointSourceID = "point-source"
    //Handicap
    private val accessibleLayerID = "handicap-layer"
    private val accessibleSourceID = "handicap-source"
    //Marker
    private val waypointMarkerLayer = "waypoint-marker-layer"
    private val destinationMarkerLayer = "destination-marker-layer"
    private val zoneLayerIDs = listOf(polygonLayerID, polygonHighlightID, pointLayerID, selectedZoneLayerID, selectedZoneHighLightID)
    //Images
    private val destinationMarker = "destination-marker-image"
    private val parkingMarker = "parking-marker-image"
    private val parkingImage = "parking-image"
    private val accessibleImage = "handicap-image"
    private lateinit var snackbar: Snackbar
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val collapsed = BottomSheetBehavior.STATE_COLLAPSED
    private val hidden = BottomSheetBehavior.STATE_HIDDEN
    private val expanded = BottomSheetBehavior.STATE_EXPANDED
    private val handler: Handler = Handler()
    //lazy inject ViewModel
    private val zoneViewModel: ZoneViewModel by sharedViewModel()
    private val selectedZoneViewModel: SelectedZoneViewModel by sharedViewModel()
    private val routeViewModel: RouteViewModel by sharedViewModel()
    private val travelViewModel: TravelViewModel by viewModel()
    private val zonePreferences: ZonePreferencesViewModel by sharedViewModel()

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
        progressBar.visibility = View.VISIBLE
        setupBottomSheet()
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(getString(R.string.streets_parking)) { style ->
                this.mapboxMap = mapboxMap.apply {
                    addOnMapClickListener(this@MapFragment)
                    addOnMapLongClickListener(this@MapFragment)
                    addOnMoveListener(this@MapFragment)
                }
                enableLocationComponent(style)
                setupImageSource(style)
                setupZoneLayers(style)
                setupMarkerLayer(style)
                setupRecyclerView()
                setupObservers()
                setupSelectedZone()
            }
        }
        setupButtons()
    }

    /** Initiates ViewModel observers */
    private fun setupObservers() {
        mainActivity.locationPermissionGranted.observeOnce(this, Observer { granted -> if(granted) enableLocationComponent(getMapStyle()!!) })
        //Observe parking zones, if changed, add them to the map and to the recyclerview.
        zoneViewModel.getStandardZones().observe(this, Observer { zones ->
            Log.d("ObserveStandard" , zones.toString())
            if (zones.isNotEmpty()) {
                addZonesToMap(zones)
                addToRecyclerView(zones)
                showSelectZone()
            } else Toast.makeText(requireContext(), "Inga zoner hittades nära din destination", Toast.LENGTH_SHORT).show()
        })
        zoneViewModel.getAccessibleZones().observe(this, Observer { zones ->
            if (zones.isNotEmpty()) {
                addMarkersToMap(FeatureCollection.fromFeatures(zones), true)
                addToRecyclerView(zones)
                showSelectZone()
            } else Toast.makeText(requireContext(), "Inga zoner hittades nära din destination", Toast.LENGTH_SHORT).show()
        })
        //Observe the selected zone, can be one from the map or the list and moves the camera to it
        selectedZoneViewModel.selectedZone.observe(this, Observer {
            val zonePoint = geometryUtils.getGeometryPoint(it.geometry())
            addSelectedZoneToMap(it)
            moveCameraToLocation(zonePoint)
        })
        //Observe an requested route, if changed this will add the route to the map and update BottomSheet
        routeViewModel.routeMap.observe(this, Observer {
            if (it.count() >= 2) {
                addRoutesToMap(it)
                updateBottomSheet(it)
                it.forEach { entry ->
                    when (entry.key) {
                        "driving" -> routeViewModel.routeDestination.changeValue(entry.value)
                        "walking" -> routeViewModel.routeWayPoint.changeValue(entry.value)
                    }
                }
            }
        })
        zonePreferences.showAccessibleZones.observe(this, Observer { showAccessibleZones ->
            val accessibleLayer = getMapStyle()?.getLayer(accessibleLayerID)
            if(showAccessibleZones) {
                hideStandardZones()
                accessibleLayer?.setProperties(visibility(VISIBLE))
            } else {
                showStandardZones()
                accessibleLayer?.setProperties(visibility(NONE))
            }
        })
    }

    /** Initiates button clickListeners */
    private fun setupButtons() {
        fab_search.setOnClickListener {
            recyclerView.visibility = View.GONE
            startAutoCompleteActivity()
        }
        my_locationFab.setOnClickListener { moveCameraToLocation() }
        expand.setOnClickListener {
            when(bottomSheetBehavior.state) {
                collapsed -> expanded
                expanded -> collapsed
            }
        }
        locate_zone.setOnClickListener { moveCameraToLocation(geometryUtils.getGeometryPoint(selectedZoneViewModel.selectedZone.value?.geometry()), duration = 3000, zoom = 16.0)  }
        startNavigationButton!!.setOnClickListener { findNavController().navigate(R.id.mapFragment_to_navigation) }
    }


    /**Initiates the RecyclerView with a adapter, clickListener, LayoutManager, Animator, SnapHelper*/
    private fun setupRecyclerView() {
        recyclerView = recycler_view
        val onItemClickListener = View.OnClickListener { recyclerView.visibility = View.GONE }
        zoneAdapter = ZoneAdapter({ zone: Feature -> zoneListItemClicked(zone) }, onItemClickListener)
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = zoneAdapter
            visibility = View.GONE
        }
    }

    /** If there is routes and a selected zone stored in the ViewModels
     * add the routes and markers to the map
     */
    private fun setupSelectedZone() {
        val zone = selectedZoneViewModel.selectedZone.value
        zone?.let {
            addMarkerOnMap(geometryUtils.getGeometryPoint(it.geometry()), true)
            addMarkerOnMap(geometryUtils.getGeometryPoint(routeViewModel.destination.value), false)
        }
    }

    /** Moves camera to either the user's location or to a selected zone, if it exists */
    private fun setupCamera() {
        selectedZoneViewModel.selectedZone.value?.let {
            val zonePoint = geometryUtils.getGeometryPoint(it.geometry())
            handler.postDelayed({
                moveCameraToLocation(zonePoint, zoom = 14.0, duration = 4000)
                progressBar.visibility = View.GONE
            }, 500)
            return
        }
       handler.postDelayed({
            moveCameraToLocation(zoom = 14.0, duration = 4000)
            // zoneViewModel.getSpecificZones(getUserLocation()!!.latitude(), getUserLocation()!!.longitude(), 1000)
            progressBar.visibility = View.GONE
        }, 500)
    }

    /** Initiates the BottomSheet with the view, BottomSheetBehaviour to control its state
     * and add a BottomSheetCallback to it.
     */
    private fun setupBottomSheet() {
        val bottomSheetCallback = getBottomSheetCallback()
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.apply {
            state = hidden
            setBottomSheetCallback(bottomSheetCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(context)) {
            // Activate the MapboxMap LocationComponent to show user location
            val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                    .trackingGesturesManagement(false)
                    .build()
            mapboxMap!!.locationComponent.apply {
                activateLocationComponent(LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
                        .locationComponentOptions(locationComponentOptions)
                        .useDefaultLocationEngine(true)
                        .build())
                isLocationComponentEnabled = true
                // Set the component's camera mode
                cameraMode = CameraMode.NONE
                setupCamera()
            }
        } else {
            mainActivity.requestLocationPermission()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapClick(point: LatLng): Boolean {
        val originPoint = getUserLocation()
        val destination = routeViewModel.destination.value
        destination?.let {
            if (queryMapClick(point)) {
                val wayPoint = Point.fromLngLat(point.longitude, point.latitude)
                val source = mapboxMap?.style?.getSourceAs<GeoJsonSource>("map-click-marker")
                source?.setGeoJson(Feature.fromGeometry(wayPoint))
                routeViewModel.getWayPointRoute(originPoint!!, wayPoint, destination)
                progressBar.visibility = View.VISIBLE
            }
        }
        return true
    }

    override fun onMapLongClick(point: LatLng): Boolean {
        routeViewModel.destination.changeValue(Point.fromLngLat(point.longitude, point.latitude))
        routeViewModel.destination.value?.let {
            moveCameraToLocation(it, animate = false)
            addMarkerOnMap(it, false)
            zoneViewModel.getSpecificZones(latitude = it.latitude(), longitude = it.longitude(), radius = 1000, fetchAccessible = zonePreferences.showAccessibleZones.value!!)
        }
        return true
    }

    /** Queryes the map for zone features on the point clicked
     *
     * @param point Location to query
     * */
    private fun queryMapClick(point: LatLng): Boolean {
        val pixel = mapboxMap?.projection?.toScreenLocation(point)
        pixel?.let {
            val features = mapboxMap?.queryRenderedFeatures(pixel, pointLayerID, polygonLayerID, accessibleLayerID)
            features?.let {
                if (features.size > 0) {
                    val feature = features[0]

                    addMarkerOnMap(Point.fromLngLat(point.longitude, point.latitude), true)
                    selectedZoneViewModel.selectedZone.changeValue(feature)
                    return true
                }
            }
        }
        return false
    }

    /** Adds a marker layer to be reused over and over
     * @param loadedMapStyle Mapbox style to add Layers and Sources to
     * */
    private fun setupMarkerLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(waypointMarkerLayer))
        loadedMapStyle.addLayer(SymbolLayer(waypointMarkerLayer, waypointMarkerLayer)
                .withProperties(iconImage(parkingMarker),
                        iconSize(0.8f),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)))
        loadedMapStyle.addSource(GeoJsonSource(destinationMarkerLayer))
        loadedMapStyle.addLayer(SymbolLayer(destinationMarkerLayer, destinationMarkerLayer)
                .withProperties(iconImage(destinationMarker),
                        iconSize(0.8f),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)))
    }

    /** Sets an marker on the given point
     * @param point Where the marker is getting set
     * @param isWayPoint if the SymbolLayer should have 2 markers or 1
     * */
    private fun addMarkerOnMap(point: Point, isWayPoint: Boolean) {
        if (isWayPoint)
            getMapStyle()?.getSourceAs<GeoJsonSource>(waypointMarkerLayer)?.setGeoJson(point)
        else
            getMapStyle()?.getSourceAs<GeoJsonSource>(destinationMarkerLayer)?.setGeoJson(routeViewModel.destination.value)
    }

    /** Creates layers for different type of zones
     * @param loadedMapStyle The style to add sources and layers to*/
    private fun setupZoneLayers(loadedMapStyle: Style) {
        val zoneLayer = FillLayer(polygonLayerID, polygonSourceID).withProperties(
                        fillColor(Color.parseColor("#0351ab")),
                        fillOpacity(0.65f))
        val highlightLayer = LineLayer(polygonHighlightID, polygonSourceID).withProperties(
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(2f),
                lineColor(Color.parseColor("#090cb0"))
        )
        val pointLayer = SymbolLayer(pointLayerID, pointSourceID).withProperties(iconImage(parkingImage), iconSize(0.35f))
        val handicapLayer = SymbolLayer(accessibleLayerID, accessibleSourceID).withProperties(iconImage(accessibleImage), iconSize(0.385f))
        val selectedZoneLayer = FillLayer(selectedZoneLayerID, selectedZoneSourceID).withProperties(
                        fillColor(Color.parseColor("#ff0900")),
                        fillOpacity(0.85f))
        val selectedHighlightLayer = LineLayer(selectedZoneHighLightID, selectedZoneSourceID).withProperties(
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(5f),
                lineColor(Color.parseColor("#ba170c"))
        )
        val layers = listOf(zoneLayer, highlightLayer, pointLayer, handicapLayer, selectedZoneLayer, selectedHighlightLayer)
        val sources =  listOf(GeoJsonSource(polygonSourceID),GeoJsonSource(pointSourceID), GeoJsonSource(accessibleSourceID), GeoJsonSource(selectedZoneSourceID))
        addLayersToStyle(loadedMapStyle, layers, sources)
    }

    /** Adds layers and sources to the map style
     * @param loadedMapStyle The style to add sources and layers to
     * @param newLayers layers to be added
     * @param sources sources for the layers to be added
     */
    private fun addLayersToStyle(loadedMapStyle: Style, newLayers: List<Layer>, sources: List<Source>) {
        with(loadedMapStyle) {
            newLayers.forEach {layer ->
                layer.minZoom = 13f
                when(layer.id) {
                    polygonLayerID -> this.addLayerAbove(layer, "road-street")
                    polygonHighlightID -> this.addLayerAbove(layer, polygonLayerID)
                    selectedZoneLayerID -> this.addLayerAbove(layer, polygonHighlightID)
                    selectedZoneHighLightID -> this.addLayerAbove(layer, selectedZoneLayerID)
                    else -> this.addLayer(layer)
                }
            }
            sources.forEach { source ->
                this.addSource(source)
            }
        }
    }

    /** Adds frequently used image sources to the map style
     * @param loadedMapStyle The style to add sources to*/
    private fun setupImageSource(loadedMapStyle: Style) {
        with(loadedMapStyle) {
            addImage(destinationMarker, BitmapFactory.decodeResource(resources, R.drawable.destination_marker))
            addImage(parkingMarker, BitmapFactory.decodeResource(resources, R.drawable.parking_marker))
            addImage(parkingImage, BitmapFactory.decodeResource(resources, R.drawable.park_blue))
            addImage(accessibleImage, BitmapFactory.decodeResource(resources, R.drawable.handicap_icon))
        }
    }

    /** Takes given FeatureCollection filters out Points and Polygons
     * and calls the appropiate method to add them to the map
     * @param features collection of features to be added
     * */
    private fun addZonesToMap(features: List<Feature>) {
        //all features that is polygons
        val polygons = features.filter { it.geometry() is Polygon }
        //all features that is points
        val points = features.filter { it.geometry() is Point }
        addPolygonsToMap(FeatureCollection.fromFeatures(polygons))
        addMarkersToMap(FeatureCollection.fromFeatures(points), false)
    }

    /** Adds a FillLayer representation of a given JSON String
     * @param featureCollection Valid FeatureCollection containing Polygon Features */
    private fun addPolygonsToMap(featureCollection: FeatureCollection) {
        val source = getMapStyle()?.getSourceAs<GeoJsonSource>(polygonSourceID)
        source?.setGeoJson(featureCollection)
    }

    /** Adds a SymbolLayer representation of a given JSON String, where the icon is a Parking Icon
     * @param featureCollection Valid JSON string containing Point Features
     * @param isHandicap indicates if the given JSON is handicap zones, used to change marker icon*/
    private fun addMarkersToMap(featureCollection: FeatureCollection, isHandicap: Boolean) {
        if (isHandicap) {
            val handicapSource = getMapStyle()?.getSourceAs<GeoJsonSource>(accessibleSourceID)
            handicapSource?.setGeoJson(featureCollection)
        } else {
            val pointSource = getMapStyle()?.getSourceAs<GeoJsonSource>(pointSourceID)
            pointSource?.setGeoJson(featureCollection)
        }
    }

    /** Adds a FillLayer representation of a selected zone
     * @param feature Valid feature */
    private fun addSelectedZoneToMap(feature: Feature) {
        val source = getMapStyle()?.getSourceAs<GeoJsonSource>(selectedZoneSourceID)
        source?.setGeoJson(feature)
    }

    /** Adds given route to the HashMap, if the HashMap contains 2 Routes
     * add them to the Map. */
    private fun addRoutesToMap(routes: HashMap<String, DirectionsRoute>) {
        navigationMapRoute.addRoutes(ArrayList<DirectionsRoute>(routes.values))
        navigationMapRoute.updateRouteVisibilityTo(true)
        progressBar.visibility = View.GONE
        startNavigationButton.visibility = View.VISIBLE
    }

    private fun showStandardZones() {
        zoneLayerIDs.forEach {
            getMapStyle()?.getLayer(it)?.setProperties(visibility(VISIBLE))
        }
    }

    private fun hideStandardZones() {
        zoneLayerIDs.forEach {
            getMapStyle()?.getLayer(it)?.setProperties(visibility(NONE))
        }
    }

    /** Starts a Search AutoComplete activity for searching locations */
    private fun startAutoCompleteActivity() {
        val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(getString(R.string.access_token))
                .placeOptions(PlaceOptions.builder()
                        .language("sv")
                        .hint(getString(R.string.search_hint))
                        .country("SE")
                        .limit(5)
                        .proximity(getUserLocation())
                        .build(PlaceOptions.MODE_CARDS))
                .build(requireActivity())
        startActivityForResult(intent, requestCodeAutoComplete)
    }


    private fun addToRecyclerView(features: List<Feature>) {
        zoneAdapter.setData(features)
        if(routeViewModel.destination.value != null) recyclerView.visibility = View.VISIBLE
        recyclerView.smoothScrollToPosition(0)
    }

    /** Checks for requestCode for the AutoComplete activity
     * Shows a snackbar to the user and changes visibility of StartNavigation Button, BottomSheet
     * and the route to hidden.*/
    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //if result code is for AutoComplete activity
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeAutoComplete) {
            handleAutoCompleteResult(data)
            navigationMapRoute.updateRouteVisibilityTo(false)
            startNavigationButton.visibility = View.GONE
            bottomSheetBehavior.state = hidden
        }
    }

    /** Handles the result given from the Search AutoComplete Activity*/
    private fun handleAutoCompleteResult(data: Intent?) {
        //Gets the place data from searched position
        val feature = PlaceAutocomplete.getPlace(data)
        feature?.let {
            routeViewModel.destination.changeValue(feature.geometry() as Point)
            routeViewModel.destination.value?.let {
                moveCameraToLocation(it, 15.0, 4000, zoom = 14.0)
                addMarkerOnMap(it, false)
                zoneViewModel.getSpecificZones(latitude = it.latitude(), longitude = it.longitude(), radius = 1000, fetchAccessible = zonePreferences.showAccessibleZones.value!!)
            }
        }
    }

    /** Called when an item in the RecyclerView is clicked
     * @param zone The list items binded object*/
    private fun zoneListItemClicked(zone: Feature) {
        if (zone != selectedZoneViewModel.selectedZone.value) {
            selectedZoneViewModel.selectedZone.changeValue(zone)
            val geometry = zone.geometry()
            val wayPoint: Point
            geometry?.let {
                wayPoint = geometryUtils.getGeometryPoint(geometry)
                addMarkerOnMap(wayPoint, true)
                val destination = routeViewModel.destination.value
                destination?.let {
                    routeViewModel.getWayPointRoute(getUserLocation()!!, wayPoint, destination)
                    progressBar.visibility = View.VISIBLE
                }
            }
        } else {
            //open up the bottomsheet again if the user clicks the same zone again, but the sheet is hidden
            if(bottomSheetBehavior.state == hidden) bottomSheetBehavior.state = collapsed
            Timber.d("Zone is equal to chosen zone")
        }
    }

    /** Moves the camera to a given Point
     * @param point point to move the camera to, default is getUserLocation
     * @param tilt tilt of the map, defualt 0
     * @param duration how long the animation should take, default 2 sec
     * @param zoom zoom level of the camera, default 14
     */
    private fun moveCameraToLocation(point: Point? = getUserLocation(), tilt: Double = 0.0, duration: Int = 2000, zoom: Double = getZoomLevel(), animate: Boolean = true) {
        point?.let {
            val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(point.latitude(), point.longitude()))
                    .zoom(zoom)
                    .tilt(tilt)
                    .build()
            if (animate) {
                mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), duration)
            } else {
                mapboxMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    /** Returns the user's last known location as a Point */
    @SuppressLint("MissingPermission")
    private fun getUserLocation(): Point? {
        mapboxMap?.let {
            it.locationComponent.lastKnownLocation?.let { location ->
                return Point.fromLngLat(location.longitude, location.latitude) ?: null
            }
        }
        return null
    }

    private fun showSelectZone() {
        snackbar = Snackbar.make(coordinator, R.string.select_zone, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.mapbox_blue))
        snackbar.show()
    }

    /** Returns the current zoom level of the map */
    private fun getZoomLevel(): Double = mapboxMap?.cameraPosition?.zoom ?: 14.0

    /** Returns the MapboxMap Style, used for manipulating how the map looks */
    private fun getMapStyle(): Style? = mapboxMap?.style

    /** Creates and returns a BottomSheetCallback object
     * Used to change the view of the BottomSheet depending on the state of it
     */
    private fun getBottomSheetCallback(): BottomSheetBehavior.BottomSheetCallback {
        return object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == expanded) {
                    bottomSheet.expand.setImageResource(R.drawable.expand_more)
                } else if (newState == collapsed) {
                    bottomSheet.expand.setImageResource(R.drawable.expand_less)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        }
    }

    /** Updates the contents of the BottomSheet with the information about the routes in routeMap
     * and zone in the ViewModel */
    private fun updateBottomSheet(routes: HashMap<String, DirectionsRoute>) {
        val selectedZone = selectedZoneViewModel.selectedZone.value
        selectedZone?.let {
            val drivingRouteDistance = routes["driving"]?.distance()
            val drivingDuration = routes["driving"]?.duration()
            val walkingRouteDistance = routes["walking"]?.distance()
            val walkingDuration = routes["walking"]?.duration()
            bottom_sheet.apply {
                if(it.hasProperty("zonecode")){
                    zoneId.text = selectedZone.getNumberProperty("zonecode").toInt().toString()
                    attr_accessible.visibility = View.GONE
                    if(it.getBooleanProperty("has_evc")) attr_ecs.visibility = View.VISIBLE else attr_ecs.visibility = View.GONE
                } else if(it.hasProperty("wkt")) {
                    zoneId.text = 999.toString()
                    attr_accessible.visibility = View.VISIBLE
                    attr_ecs.visibility = View.GONE
                }
                dialogZoneName.text = selectedZone.getStringProperty("zone_name")
                zoneOwner.text = selectedZone.getStringProperty("zone_owner")
                travelTime.text = travelViewModel.getTotalTravelTime(drivingDuration!!, walkingDuration!!)
                arrivalTime.text = travelViewModel.getArrivalTime(drivingDuration, walkingDuration)
                drivingDistance.text = travelViewModel.getDrivingDistance(drivingRouteDistance!!)
                drivingTime.text = travelViewModel.getDrivingTime(drivingDuration)
                walkingDistance.text = travelViewModel.getWalkingDistance(walkingRouteDistance!!)
                walkingTime.text = travelViewModel.getWalkingTime(walkingDuration)
            }
            bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
            bottomSheetBehavior.state = collapsed
        }
    }

    private fun isZonesNull(): Boolean = (zoneViewModel.getStandardZones().value.isNullOrEmpty() && selectedZoneViewModel.selectedZone.value != null)

    override fun onMoveBegin(detector: MoveGestureDetector) {
        mapboxMap?.let {
            if (it.cameraPosition.zoom < 13 && !isZonesNull()) {
                Toast.makeText(requireContext(), "Zooma in mer för att se fler zoner", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMove(detector: MoveGestureDetector) {
    }

    override fun onMoveEnd(detector: MoveGestureDetector) {
    }


    /**  ------ LifeCycle Methods ------*/
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
        handler.removeCallbacksAndMessages(null)
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

    override fun onDestroyView() {
        super.onDestroyView()
        mapboxMap?.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        private const val TAG = "MapFragment"
        val geometryUtils = GeometryUtils()
    }

}
