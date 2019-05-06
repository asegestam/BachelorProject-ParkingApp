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
import com.example.smspark.viewmodels.RouteViewModel
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.example.smspark.viewmodels.TravelViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
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
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.selected_zone.*
import kotlinx.android.synthetic.main.selected_zone.view.*
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber


class MapFragment : Fragment(), MapboxMap.OnMapClickListener, MapboxMap.OnMapLongClickListener, PermissionsListener, MapboxMap.OnMoveListener {
    // variables for adding location layer
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private val REQUEST_CODE_AUTOCOMPLETE = 1
    // variables for adding location layer
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    // variables for calculating and drawing a route
    private var navigationMapRoute: NavigationMapRoute? = null
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
    private val collapsed = BottomSheetBehavior.STATE_COLLAPSED
    private val hidden = BottomSheetBehavior.STATE_HIDDEN
    private val expanded = BottomSheetBehavior.STATE_EXPANDED
    //lazy inject ViewModel
    private val zoneViewModel: ZoneViewModel by sharedViewModel()
    private val selectedZoneViewModel: SelectedZoneViewModel by sharedViewModel()
    private val routeViewModel: RouteViewModel by sharedViewModel()
    private val travelViewModel: TravelViewModel by viewModel()

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
        initBottomSheet()
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
                initRecyclerView()
                initObservers()
                initSelectedZone()
            }
        }
        initButtons()
    }

    /** Initiates ViewModel observers */
    private fun initObservers() {
        //Observe parking zones, if changed, add them to the map and to the recyclerview.
        zoneViewModel.getAllZones().observe(this, Observer { hashMap ->
            if (checkZoneUpdate(hashMap)) {
                hashMap["standard"]?.let { addZonesToMap(it) }
                hashMap["accessible"]?.let { addMarkersToMap(it, true) }
                addToRecyclerView(hashMap)
            } else Toast.makeText(requireContext(), "Inga zoner hittades nära din destination", Toast.LENGTH_SHORT).show()
        })
        //Observe the selected zone, can be one from the map or the list and moves the camera to it
        selectedZoneViewModel.selectedZone.observe(this, Observer {
            val zonePoint = geometryUtils.getGeometryPoint(it.geometry())
            moveCameraToLocation(zonePoint)
        })
        //Observe an requested route, if changed this will add the route to the map and update BottomSheet
        routeViewModel.routeMap.observe(this, Observer {
            if (it.count() >= 2) {
                addRoutesToMap(it)
                updateBottomSheet(it)
                it.forEach { entry ->
                    when (entry.key) {
                        "driving" -> routeViewModel.routeDestination.value = entry.value
                        "walking" -> routeViewModel.routeWayPoint.value = entry.value
                    }
                }
            }
        })
    }

    /** Initiates button clickListeners */
    private fun initButtons() {
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
    private fun initRecyclerView() {
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
    private fun initSelectedZone() {
        val routes = routeViewModel.routeMap.value
        val zone = selectedZoneViewModel.selectedZone.value
        if (!routes.isNullOrEmpty()) {
            addRoutesToMap(routes)
            updateBottomSheet(routes)
        }
        zone?.let {7
            addMarkerOnMap(geometryUtils.getGeometryPoint(it.geometry()), true)
            navigationMapRoute?.updateRouteVisibilityTo(true)
        }
    }

    /** Moves camera to either the user's location or to a selected zone, if it exists */
    private fun initCamera() {
        selectedZoneViewModel.selectedZone.value?.let {
            val zonePoint = geometryUtils.getGeometryPoint(it.geometry())
            Handler().postDelayed({
                moveCameraToLocation(zonePoint, zoom = 14.0, duration = 4000)
                progressBar.visibility = View.GONE
            }, 500)
            return
        }
        Handler().postDelayed({
            moveCameraToLocation(zoom = 14.0, duration = 4000)
            // zoneViewModel.getSpecificZones(getUserLocation()!!.latitude(), getUserLocation()!!.longitude(), 1000)
            progressBar.visibility = View.GONE
        }, 500)
    }

    /** Initiates the BottomSheet with the view, BottomSheetBehaviour to control its state
     * and add a BottomSheetCallback to it.
     */
    private fun initBottomSheet() {
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
                initCamera()
            }
        } else {
            permissionsManager.requestLocationPermissions(requireActivity())
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
        routeViewModel.destination.value = Point.fromLngLat(point.longitude, point.latitude)
        routeViewModel.destination.value?.let {
            moveCameraToLocation(it, animate = false)
            addMarkerOnMap(it, false)
            zoneViewModel.getSpecificZones(latitude = it.latitude(), longitude = it.longitude(), radius = 1000)
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
            val features = mapboxMap?.queryRenderedFeatures(pixel, pointLayer, polygonLayer, handicapLayer)
            features?.let {
                if (features.size > 0) {
                    val feature = features[0]
                    addMarkerOnMap(Point.fromLngLat(point.longitude, point.latitude), true)
                    selectedZoneViewModel.selectedZone.value = feature
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
        loadedMapStyle.addSource(GeoJsonSource(markerSource))
        loadedMapStyle.addLayer(SymbolLayer("marker-layer", markerSource)
                .withProperties(iconImage(markerImage),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)))
    }

    /** Sets an marker on the given point
     * @param point Where the marker is getting set
     * @param isWayPoint if the SymbolLayer should have 2 markers or 1
     * */
    private fun addMarkerOnMap(point: Point, isWayPoint: Boolean) {
        val source = getMapStyle()?.getSourceAs<GeoJsonSource>(markerSource)
        if (source != null) {
            if (isWayPoint) {
                source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(routeViewModel.destination.value), Feature.fromGeometry(point))))
            } else {
                source.setGeoJson(routeViewModel.destination.value)
            }
        }
    }

    /** Adds frequently used GeoJson sources and layers to the map
     * @param loadedMapStyle The style to add sources and layers to*/
    private fun setupZoneLayers(loadedMapStyle: Style) {
        val polygonLayer = FillLayer(polygonLayer, polygonSource)
                .withProperties(
                        fillColor(Color.parseColor("#f42428")),
                        fillOpacity(0.75f))
        val pointLayer = SymbolLayer(pointLayer, pointSource)
                .withProperties(iconImage(parkingImage), iconSize(0.35f))
        val handicapLayer = SymbolLayer(handicapLayer, handicapSource)
                .withProperties(iconImage(handicapImage), iconSize(0.8f))
        with(loadedMapStyle) {
            addSource(GeoJsonSource(polygonSource))
            addSource(GeoJsonSource(pointSource))
            addSource(GeoJsonSource(handicapSource))
            polygonLayer.minZoom = 13f
            pointLayer.minZoom = 13f
            handicapLayer.minZoom = 13f
            addLayerAbove(polygonLayer, "road-street")
            addLayer(pointLayer)
            addLayer(handicapLayer)
        }
    }

    /** Adds frequently used image sources to the map style
     * @param loadedMapStyle The style to add sources to*/
    private fun setupImageSource(loadedMapStyle: Style) {
        with(loadedMapStyle) {
            addImage(markerImage, BitmapFactory.decodeResource(resources, R.drawable.mapbox_marker_icon_default))
            addImage(parkingImage, BitmapFactory.decodeResource(resources, R.drawable.park_blue))
            addImage(handicapImage, BitmapFactory.decodeResource(resources, R.drawable.accessible_png))
        }
    }

    /** Takes given FeatureCollection filters out Points and Polygons
     * and calls the appropiate method to add them to the map
     * @param featureCollection collection of features to be added
     * */
    private fun addZonesToMap(featureCollection: FeatureCollection) {
        val features = featureCollection.features()
        //all features that is polygons
        val polygons = features?.filter { it.geometry() is Polygon }
        //all features that is points
        val points = features?.filter { it.geometry() is Point }
        polygons?.let { addPolygonsToMap(FeatureCollection.fromFeatures(polygons)) }
        points?.let { addMarkersToMap(FeatureCollection.fromFeatures(points), false) }
    }

    /** Adds a FillLayer representation of a given JSON String
     * @param featureCollection Valid JSON string containing Polygon Features */
    private fun addPolygonsToMap(featureCollection: FeatureCollection) {
        val source = getMapStyle()?.getSourceAs<GeoJsonSource>(polygonSource)
        source?.setGeoJson(featureCollection)
    }

    /** Adds a SymbolLayer representation of a given JSON String, where the icon is a Parking Icon
     * @param featureCollection Valid JSON string containing Point Features
     * @param isHandicap indicates if the given JSON is handicap zones, used to change marker icon*/
    private fun addMarkersToMap(featureCollection: FeatureCollection, isHandicap: Boolean) {
        if (isHandicap) {
            val handicapSource = getMapStyle()?.getSourceAs<GeoJsonSource>(handicapSource)
            handicapSource?.setGeoJson(featureCollection)
        } else {
            val pointSource = getMapStyle()?.getSourceAs<GeoJsonSource>(pointSource)
            pointSource?.setGeoJson(featureCollection)
        }
    }

    /** Adds given route to the HashMap, if the HashMap contains 2 Routes
     * add them to the Map. */
    private fun addRoutesToMap(routes: HashMap<String, DirectionsRoute>) {
        if (navigationMapRoute == null) {
            navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap!!, R.style.NavigationMapRoute)
        }
        navigationMapRoute?.addRoutes(ArrayList<DirectionsRoute>(routes.values))
        progressBar.visibility = View.GONE
        startNavigationButton.visibility = View.VISIBLE
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
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
    }

    private fun checkZoneUpdate(hashMap: HashMap<String,FeatureCollection>): Boolean {
        return !hashMap["standard"]?.features().isNullOrEmpty()
    }

    private fun addToRecyclerView(hashMap: HashMap<String, FeatureCollection>) {
        val list = arrayListOf<Feature>()
        hashMap.values.forEach {
            it.features()?.let { listOfFeatures -> list.addAll(listOfFeatures) }
        }
        zoneAdapter.setData(FeatureCollection.fromFeatures(list))
        recyclerView.visibility = View.VISIBLE
        recyclerView.smoothScrollToPosition(0)
    }

    /** Checks for requestCode for the AutoComplete activity
     * Shows a snackbar to the user and changes visibility of StartNavigation Button, BottomSheet
     * and the route to hidden.*/
    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //if result code is for AutoComplete activity
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            handleAutoCompleteResult(data)
            navigationMapRoute?.updateRouteVisibilityTo(false)
            startNavigationButton.visibility = View.GONE
            snackbar = Snackbar.make(coordinator, R.string.select_zone, Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            snackbarView.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.mapbox_blue))
            snackbar.show()
            bottomSheetBehavior.state = hidden
        }
    }

    /** Handles the result given from the Search AutoComplete Activity*/
    private fun handleAutoCompleteResult(data: Intent?) {
        //Gets the place data from searched position
        val feature = PlaceAutocomplete.getPlace(data)
        feature?.let {
            routeViewModel.destination.value = feature.geometry() as Point
            routeViewModel.destination.value?.let {
                moveCameraToLocation(it, 15.0, 4000, zoom = 14.0)
                addMarkerOnMap(it, false)
                zoneViewModel.getSpecificZones(latitude = it.latitude(), longitude = it.longitude(), radius = 1000)
            }
        }
    }

    /** Called when an item in the RecyclerView is clicked
     * @param zone The list items binded object*/
    private fun zoneListItemClicked(zone: Feature) {
        if (zone != selectedZoneViewModel.selectedZone.value) {
            selectedZoneViewModel.selectedZone.value = zone
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
                zoneName.text = selectedZone.getStringProperty("zone_name")
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

    override fun onMoveBegin(detector: MoveGestureDetector) {
        mapboxMap?.let {
            if (it.cameraPosition.zoom < 13 && zoneViewModel.getAllZones().value != null && selectedZoneViewModel.selectedZone.value == null) {
                Toast.makeText(requireContext(), "Zooma in mer för att se fler zoner", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMove(detector: MoveGestureDetector) {
    }

    override fun onMoveEnd(detector: MoveGestureDetector) {
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(requireContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Log.d("onPermissionResult", "permission granted")
            enableLocationComponent(getMapStyle()!!)
        } else {
            Toast.makeText(requireContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show()
            permissionsManager.requestLocationPermissions(requireActivity())
        }
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
        Handler().removeCallbacksAndMessages(null)
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
