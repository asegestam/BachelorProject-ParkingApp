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
import com.example.smspark.viewmodels.RouteViewModel
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
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
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MapFragment : Fragment(), MapboxMap.OnMapClickListener, PermissionsListener, MapboxMap.OnMoveListener {
    // variables for adding location layer
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private val REQUEST_CODE_AUTOCOMPLETE = 1
    // variables for adding location layer
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    // variables for calculating and drawing a route
    private var navigationMapRoute: NavigationMapRoute? = null
    private var routeMap = HashMap<String, DirectionsRoute>()
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
    val selectedZoneViewModel: SelectedZoneViewModel by sharedViewModel()
    private val routeViewModel: RouteViewModel by sharedViewModel()

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
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(getString(R.string.streets_parking)) {style ->
                this.mapboxMap = mapboxMap
                mapboxMap.addOnMapClickListener(this)
                mapboxMap.addOnMoveListener(this)
                enableLocationComponent(style)
                setupImageSource(style)
                setupZoneLayers(style)
                setupMarkerLayer(style)
                checkTripFragment()
                initRecyclerView()
                initObservers()
                initCamera()
                initSelectedZone()
            }
        }
        initButtons()
        initBottomSheet()
    }

    /**Checks if there is an argument bundle from TripFragment, if it exists fetch zones and a route */
    private fun checkTripFragment() {
        arguments?.let {
            val fromPoint = Point.fromJson(it.getString("fromArg"))
            val destinationPoint = Point.fromJson(it.getString("destArg"))
            val wayPoint = Point.fromJson(it.getString("wayPointArg"))
            val wayPointFeature = Feature.fromJson(it.getString("wayPointFeatureArg"))
            zoneViewModel.getSpecificZones(destinationPoint.latitude(), destinationPoint.longitude(), 1000)
            routeViewModel.getWayPointRoute(fromPoint, wayPoint, destinationPoint)
            addMarkerOnMap(destinationPoint, false)
            addMarkerOnMap(wayPoint, true)
            selectedZoneViewModel.selectedZone.value = wayPointFeature
        }
    }

    /** Initiates ViewModel observers */
    private fun initObservers() {
        //Observe parking zones, if changed, add them to the map and to the list.
       zoneViewModel.getObservableZones().observe(this, Observer { featureCollection -> featureCollection.features()?.let {
            if(it.size > 0) {
                addZonesToMap(featureCollection)
                zoneAdapter.setData(featureCollection)
                recyclerView.visibility = View.VISIBLE
                recyclerView.smoothScrollToPosition(0)
            } else {
                Toast.makeText(requireContext(), "Inga zoner hittades", Toast.LENGTH_LONG).show()
            }
        }})
        //Observe handicap parking zones, if changed, add them to the map and to the list.
        zoneViewModel.getObservableHandicapZones().observe(this, Observer {featureCollection -> featureCollection.features()?.let {
            if(it.size > 0) {
                addMarkersToMap(featureCollection, true)
                zoneAdapter.setData(featureCollection)
                recyclerView.visibility = View.VISIBLE
                recyclerView.smoothScrollToPosition(0)
            } else {
                Toast.makeText(requireContext(), "Inga handikapp-zoner hittades", Toast.LENGTH_LONG).show()
            }
        }})
        /*
        Observe the selected zone, can be one from the map or the list,
        will open up BottomSheet to show zone info and move camera to its location
         */
        selectedZoneViewModel.selectedZone.observe(this, Observer {
            bottomSheetBehavior.state = collapsed
            val zonePoint = getGeometryPoint(it.geometry())
            moveCameraToLocation(zonePoint, zoom = 14.0)
        })
        //Observe an requested route, if changed this will add the route to the map
        routeViewModel.getRoute().observe(this, Observer { route -> addRouteToMap(route) })
    }

    /** Initiates button clickListeners */
    private fun initButtons() {
        fab_search.setOnClickListener {
            recyclerView.visibility = View.GONE
            startAutoCompleteActivity() }
        my_locationFab.setOnClickListener { moveCameraToLocation() }
        expandLess.setOnClickListener { bottomSheetBehavior.state = collapsed }
        expandMore.setOnClickListener { bottomSheetBehavior.state = expanded }
        startNavigationButton!!.setOnClickListener { findNavController().navigate(R.id.mapFragment_to_navigation) }
    }

    /**Initiates the RecyclerView with a adapter, clickListener, LayoutManager, Animator, SnapHelper*/
    private fun initRecyclerView() {
        recyclerView = recycler_view
        recyclerView.setHasFixedSize(true)
        val onItemClickListener = View.OnClickListener { recyclerView.visibility = View.GONE }
        zoneAdapter = ZoneAdapter(context!!, { zone: Feature -> zoneListItemClicked(zone)}, onItemClickListener)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = zoneAdapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
    }

    /** If there is routes and a selected zone stored in the ViewModels
     * add the routes and markers to the map
     */
    private fun initSelectedZone() {
        val destinationRoute = routeViewModel.routeDestination.value
        val wayPointRoute = routeViewModel.routeWayPoint.value
        val zone = selectedZoneViewModel.selectedZone.value
        if (destinationRoute != null && wayPointRoute != null) {
            addRouteToMap(destinationRoute)
            addRouteToMap(wayPointRoute)
        }
        zone?.let { addMarkerOnMap(getGeometryPoint(it.geometry()), true) }
    }

    /** Moves camera to either the user's location or to a selected zone, if it exists */
    private fun initCamera() {
        selectedZoneViewModel.selectedZone.value?.let {
            val zonePoint = getGeometryPoint(it.geometry())
            Handler().postDelayed({
                moveCameraToLocation(zonePoint , zoom = 14.0)
            }, 1000)
            return
        }
        Handler().postDelayed({
            moveCameraToLocation()
        }, 1000)
    }

    /** Initiates the BottomSheet with the view, BottomSheetBehaviour to control its state
     * and add a BottomSheetCallback to it.
     */
    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.state = hidden
        val bottomSheetCallback = getBottomSheetCallback()
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback)
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
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(activity as MainActivity)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapClick(point: LatLng): Boolean {
        val originPoint = getUserLocation()
        bottomSheetBehavior.state = hidden
        val destination = routeViewModel.destination.value
        destination?.let {
            if (queryMapClick(point)) {
                val wayPoint = Point.fromLngLat(point.longitude, point.latitude)
                val source = mapboxMap?.style?.getSourceAs<GeoJsonSource>("map-click-marker")
                source?.setGeoJson(Feature.fromGeometry(wayPoint))
                routeViewModel.getWayPointRoute(originPoint!!, wayPoint, destination)
            }
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
                if(features.size > 0) {
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
        if(source != null) {
            if(isWayPoint) {
                source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(routeViewModel.destination.value), Feature.fromGeometry(point))))
            } else {
                source.setGeoJson(routeViewModel.destination.value)
            }
        }
    }

    /** Adds frequently used GeoJson sources and layers to the map
     * @param loadedMapStyle The style to add sources and layers to*/
    private fun setupZoneLayers(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(polygonSource))
        loadedMapStyle.addSource(GeoJsonSource(pointSource))
        loadedMapStyle.addSource(GeoJsonSource(handicapSource))
        val polygonLayer = FillLayer(polygonLayer, polygonSource)
                .withProperties(
                        fillColor(Color.parseColor("#f42428")),
                        fillOpacity(0.75f))
        val pointLayer = SymbolLayer(pointLayer, pointSource)
                .withProperties(iconImage(parkingImage), iconSize(0.35f))
        val handicapLayer = SymbolLayer(handicapLayer, handicapSource)
                        .withProperties(iconImage(handicapImage), iconSize(0.8f))
        polygonLayer.minZoom = 13f
        pointLayer.minZoom = 13f
        handicapLayer.minZoom = 13f
        loadedMapStyle.addLayerAbove(polygonLayer, "road-street")
        loadedMapStyle.addLayer(pointLayer)
        loadedMapStyle.addLayer(handicapLayer)
    }

    /** Adds frequently used image sources to the map style
     * @param loadedMapStyle The style to add sources to*/
    private fun setupImageSource(loadedMapStyle: Style) {
        loadedMapStyle.addImage(markerImage, BitmapFactory.decodeResource(resources, R.drawable.mapbox_marker_icon_default))
        loadedMapStyle.addImage(parkingImage, BitmapFactory.decodeResource(resources, R.drawable.park_blue))
        loadedMapStyle.addImage(handicapImage, BitmapFactory.decodeResource(resources, R.drawable.handicap_icon))
    }

    /** Takes given FeatureCollection filters out Points and Polygons
     * and calls the appropiate method to add them to the map
     * @param featureCollection collection of features to be added
     * */
    private fun addZonesToMap(featureCollection: FeatureCollection) {
        val features = featureCollection.features()
        //all features that is polygons
        val polygons = features?.filter { it.geometry() is Polygon}
        //all features that is points
        val points = features?.filter { it.geometry() is Point}
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
        if(isHandicap) {
            val handicapSource = getMapStyle()?.getSourceAs<GeoJsonSource>(handicapSource)
            handicapSource?.setGeoJson(featureCollection)
        } else {
            val pointSource = getMapStyle()?.getSourceAs<GeoJsonSource>(pointSource)
            pointSource?.setGeoJson(featureCollection)
        }
    }

    private fun addRouteToMap(route: DirectionsRoute) {
        if(navigationMapRoute == null) {
            navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap!!, R.style.NavigationMapRoute)
        }
        val profile = route.routeOptions()?.profile()
        profile?.let {
            when(it) {
                "driving" -> routeViewModel.routeDestination.postValue(route)
                "walking" -> routeViewModel.routeWayPoint.postValue(route)
            }
            routeMap[it] = route
            navigationMapRoute?.addRoutes(ArrayList<DirectionsRoute>(routeMap.values))
            startNavigationButton.visibility = View.VISIBLE
        }
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
            routeViewModel.destination.value = feature.geometry() as Point
            routeViewModel.destination.value?.let {
                moveCameraToLocation(it, 15.0, 2000, zoom = 14.0)
                addMarkerOnMap(it, false)
                zoneViewModel.getSpecificZones(latitude = it.latitude(), longitude = it.longitude(), radius = 1000)
            }
        }
    }

    /** Called when an item in the RecyclerView is clicked
     * @param zone The list items binded object*/
    private fun zoneListItemClicked(zone: Feature) {
        if(zone != selectedZoneViewModel.selectedZone.value && selectedZoneViewModel.selectedZone.value != null ) {
            bottomSheetBehavior.state = hidden
            selectedZoneViewModel.selectedZone.value = zone
            val geometry = zone.geometry()
            val wayPoint: Point
            geometry?.let {
                wayPoint = getGeometryPoint(geometry)
                addMarkerOnMap(wayPoint, true)
                val destination = routeViewModel.destination.value
                destination?.let {
                    routeViewModel.getWayPointRoute(getUserLocation()!!, wayPoint, destination!!)

                }
            }
        } else {
            Timber.d("Zone is equal to chosen zone")
        }
    }

    /** Returns a point of the given geometry */
    private fun getGeometryPoint(geometry: Geometry?) : Point {
        return when (geometry) {
            is Polygon -> getPolygonCenter(geometry)
            is MultiPolygon -> getMultiPolygonCenter(geometry)
            else -> geometry as Point
        }
    }

    /** Returns a middle point of a given Geometry, only used for polygons */
    private fun getPolygonCenter(geometry: Geometry): Point {
        val  builder  = LatLngBounds.Builder()
        val polygon = geometry as Polygon
        polygon.outer()?.coordinates()?.forEach {
            builder.include(LatLng(it.latitude(), it.longitude()))
        }
        val center = builder.build().center
        return Point.fromLngLat(center.longitude, center.latitude)
    }

    /** Returns a middle point of a given Geometry, only used for MultiPolygons */
    private fun getMultiPolygonCenter(geometry: Geometry): Point {
        val builder = LatLngBounds.Builder()
        val multiPolygon = geometry as MultiPolygon
        multiPolygon.coordinates()[0][0].forEach { point ->
            builder.include(LatLng(point.latitude(), point.longitude()))
        }
        val center = builder.build().center
        return Point.fromLngLat(center.longitude, center.latitude)
    }

    private fun moveCameraToLocation(point: Point? = getUserLocation(), tilt: Double = 0.0, duration: Int = 2000, zoom: Double = 14.0) {
        point?.let {
            mapboxMap?.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                    .target(LatLng(point.latitude(), point.longitude()))
                    .zoom(zoom)
                    .tilt(tilt)
                    .build()))
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(): Point? {
        mapboxMap?.let {
            it.locationComponent.apply {
                if(isLocationComponentEnabled) {
                    lastKnownLocation?.let {location ->
                        return Point.fromLngLat(location.longitude, location.latitude)
                    }
                }
            }
        }
        return null
    }

    private fun getMapStyle(): Style? = mapboxMap?.style

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
            requireActivity().finish()
        }
    }

    /** Creates and returns a BottomSheetCallback object
     * Used to set the right information into the BottomSheet depending on the Zone
     */
    private fun getBottomSheetCallback() : BottomSheetBehavior.BottomSheetCallback {
        return object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val selectedZone = selectedZoneViewModel.selectedZone.value
                if(newState == collapsed)
                    selectedZone?.let {
                        bottomSheet.zoneId.text = selectedZone.getNumberProperty("zonecode").toInt().toString()
                        bottomSheet.zoneName.text = selectedZone.getStringProperty("zone_name")
                        bottomSheet.zoneOwner.text = selectedZone.getStringProperty("zone_owner")
                        bottomSheet.travelLength.text = calcEstimatedTravelLength()
                        bottomSheet.travelTime.text = calcEstimatedTravelTime()
                        bottomSheet.parkingDistance.text= selectedZone.getNumberProperty("distance").toInt().toString()
                    }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        }
    }

    private fun calcEstimatedTravelTime(): String {
        var totalTime = .0
        ArrayList<DirectionsRoute>(routeMap.values).forEach{
            totalTime += it.duration()!!
        }
        val timeInMinutes = TimeUnit.SECONDS.toMinutes(totalTime.toLong()).toDouble()
        return "%.2f".format(timeInMinutes)
    }

    private fun calcEstimatedTravelLength() : String {
        var totalLength = .0
        ArrayList<DirectionsRoute>(routeMap.values).forEach{
            totalLength += it.distance()!!
        }
        return "%.2f".format(totalLength/1000)
    }

    override fun onMoveBegin(detector: MoveGestureDetector) {
        mapboxMap?.let {
            if(it.cameraPosition.zoom < 13 && zoneViewModel.getObservableZones().value != null) {
                Toast.makeText(requireContext(), "Zooma in för att se zoner", Toast.LENGTH_SHORT).show()
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
    }
}
