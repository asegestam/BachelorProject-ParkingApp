package com.example.smspark.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
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
import com.example.smspark.model.RouteViewModel
import com.example.smspark.model.ZoneModel.ZoneAdapter
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
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
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.selected_zone.*
import kotlinx.android.synthetic.main.selected_zone.view.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class MapFragment : Fragment(), MapboxMap.OnMapClickListener, PermissionsListener {

    // variables for adding location layer
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private val REQUEST_CODE_AUTOCOMPLETE = 1
    // variables for adding location layer
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationComponent: LocationComponent
    // variables for calculating and drawing a route
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
    private val zoneViewModel: ZoneViewModel by sharedViewModel()
    val selectedZoneViewModel: SelectedZoneViewModel by viewModel()
    private val routeViewModel: RouteViewModel by inject { parametersOf(requireContext()) }

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
                enableLocationComponent(style)
                setupImageSource(style)
                setupZoneLayers(style)
                setupMarkerLayer(style)
                initRecyclerView()
                initObservers()
            }
        }
        initButtons()
        initBottomSheets()
    }

    /** Initiates ViewModel observers */
    private fun initObservers() {
        zoneViewModel.getHandicapZones().observe(this, Observer {
            addMarkersToMap(it, true)
            zoneAdapter.setData(it)
        })
        zoneViewModel.getSpecificZones().observe(this, Observer { featureCollection -> featureCollection.features()?.let {
            if(it.size > 0) {
                addZonesToMap(featureCollection)
                zoneAdapter.setData(featureCollection)
            } else {
                Toast.makeText(requireContext(), "Inga zoner hittades", Toast.LENGTH_LONG).show()
            }
        }})
        selectedZoneViewModel.selectedZone.observe(this, Observer { bottomSheetBehavior.state = COLLAPSED})
        routeViewModel.route.observe(this, Observer { route -> handleRoute(route) })
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
        zoneAdapter = ZoneAdapter(context!!) { zone: Feature -> zoneListItemClicked(zone) }
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = zoneAdapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
    }

    private fun initBottomSheets() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.state = HIDDEN
        val bottomSheetCallback = getBottomSheetCallback()
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.locationComponent
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
                    .useDefaultLocationEngine(true)
                    .build())
            locationComponent.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING_GPS
            locationComponent.renderMode = RenderMode.COMPASS
            moveCameraToLocation()
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
            val source = mapboxMap.style?.getSourceAs<GeoJsonSource>("map-click-marker")
            source?.setGeoJson(Feature.fromGeometry(wayPoint))
            routeViewModel.getWayPointRoute(originPoint, wayPoint, destination!!, "driving")
        }
        return true
    }

    /** Queryes the map for zone features on the point clicked
     *
     * @param point Location to query
     * */
    private fun queryMapClick(point: LatLng): Boolean {
        val pixel = mapboxMap.projection.toScreenLocation(point)
        val features = mapboxMap.queryRenderedFeatures(pixel, pointLayer, polygonLayer, handicapLayer)
        if(features.size > 0) {
            Timber.d( "features queryed " + features.size)
            val feature = features[0]
            addMarkerOnMap(Point.fromLngLat(point.longitude, point.latitude), true)
            selectedZoneViewModel.selectedZone.value = feature
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
        val source = getMapStyle()?.getSourceAs<GeoJsonSource>(markerSource)
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

    /** Takes given FeatureCollection filters out Points and Polygons
     * and calls the appropiate method to add them to the map
     * @param featureCollection collection of features to be added
     * */
    private fun addZonesToMap(featureCollection: FeatureCollection) {
        Timber.d("addZonesToMap " + featureCollection.toString())
        val features = featureCollection.features()
        //all features that is polygons
        val polygons = features?.filter { it.geometry() is Polygon}
        //all features that is points
        val points = features?.filter { it.geometry() is Point}
        Timber.d("Points " + points.toString())
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

    private fun handleRoute(route: DirectionsRoute) {
        navigationMapRoute = NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute)
        navigationMapRoute?.addRoute(route)
        startNavigationButton.visibility = View.VISIBLE
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
            snackbar = Snackbar.make(coordinator, R.string.select_zone , Snackbar.LENGTH_SHORT)
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
            destination?.let {
                moveCameraToLocation(it, 15.0, 2000, zoom = 16.0)
                addMarkerOnMap(it, false)
                zoneViewModel.getSpecificZones(latitude = it.latitude(), longitude = it.longitude(), radius = 2000)
            }
        }
    }

    /** Called when an item in the RecyclerView is clicked
     * @param zone The list items binded object*/
    private fun zoneListItemClicked(zone: Feature) {
        if(zone != selectedZoneViewModel.selectedZone.value) {
            bottomSheetBehavior.state = HIDDEN
            selectedZoneViewModel.selectedZone.value = zone
            val geometry = zone.geometry()
            val wayPoint: Point
            if(geometry is Polygon) {
                //geometry of clicked zone is polygon, get one of the points to add a marker
                val polygonCoordinates = geometry.coordinates()
                wayPoint = polygonCoordinates[0][0]
                addMarkerOnMap(wayPoint, true)
            } else {
                //if its not a polygon then its already a point
                wayPoint = geometry as Point
                addMarkerOnMap(wayPoint, true)
            }
            if(destination != null) {
                routeViewModel.getWayPointRoute(getUserLocation(), wayPoint, destination!!, "driving")
            }
        } else {
            Timber.d("Zone is equal to chosen zone")
        }
    }

    private fun moveCameraToLocation(point: Point? = getUserLocation(), tilt: Double = 0.0, duration: Int = 2000, zoom: Double = 14.0) {
        point?.let {
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                    .target(LatLng(point.latitude(), point.longitude()))
                    .zoom(zoom)
                    .tilt(tilt)
                    .build()), duration)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(): Point? {
        if(locationComponent.isLocationComponentEnabled) {
            val location = locationComponent.lastKnownLocation
            location?.let { return Point.fromLngLat(it.longitude, it.latitude) }
        }
        return null
    }

    private fun getMapStyle(): Style? = mapboxMap.style

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(requireContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
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
                if(newState == COLLAPSED) {
                    val selectedZone = selectedZoneViewModel.selectedZone.value
                    selectedZone?.let {
                        if(selectedZone.hasProperty("zonecode")) {
                            bottomSheet.zoneName.text = selectedZone.getStringProperty("zone_name")
                            bottomSheet.zoneType.text = getString(R.string.zon_kod) + selectedZone.getNumberProperty("zonecode").toInt()
                            bottomSheet.zoneOwner.text = selectedZone.getStringProperty("zone_owner")
                            bottomSheet.zoneDistance.text = selectedZone.getNumberProperty("distance").toInt().toString() + " m"
                        } else {
                            bottomSheet.zoneName.text = selectedZone.getStringProperty("Name")
                            bottomSheet.zoneType.text = getString(R.string.handicap)
                            bottomSheet.zoneOwner.text = selectedZone.getStringProperty("Owner")
                            bottomSheet.zoneDistance.text = selectedZone.getNumberProperty("Distance").toInt().toString() + " m"
                        }
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
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
        mapboxMap.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
