package com.example.smspark.views


import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.smspark.R
import com.example.smspark.viewmodels.RouteViewModel
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.ui.PlaceAutocompleteFragment
import com.mapbox.mapboxsdk.plugins.places.autocomplete.ui.PlaceSelectionListener
import kotlinx.android.synthetic.main.destination_search.*
import kotlinx.android.synthetic.main.fragment_trip.*
import org.koin.android.viewmodel.ext.android.sharedViewModel

@SuppressLint("MissingPermission")
class TripFragment : Fragment() {

    private lateinit var fromPoint: Point
    private lateinit var toPoint: Point
    private lateinit var autoCompleteFragment: PlaceAutocompleteFragment
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    //lazy inject ViewModel
    private val zoneViewModel: ZoneViewModel by sharedViewModel()
    private val routeViewModel: RouteViewModel by sharedViewModel()
    private val selectedZoneViewModel: SelectedZoneViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        autoCompleteFragment = PlaceAutocompleteFragment.newInstance(getString(R.string.access_token), PlaceOptions.builder()
                                .language("sv")
                                .hint(getString(R.string.search_hint))
                                .country("SE")
                                .proximity(Point.fromLngLat(location.longitude, location.latitude))
                                .build(PlaceOptions.MODE_CARDS))
                    }
                }
        initComponents()
    }


    private fun initAutoComplete(fragment: PlaceAutocompleteFragment, tag: String) {
        fragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(carmenFeature: CarmenFeature) {
                    when(tag) {
                        "to" -> handleToLocation(carmenFeature)
                        "from" -> handleFromLocation(carmenFeature)
                    }
                    removeAutoCompleteFragment()
                }
                override fun onCancel() {
                    removeAutoCompleteFragment()
                }
            })
    }

    private fun addAutoCompleteFragment(fragment: PlaceAutocompleteFragment, tag: String) {
        activity?.supportFragmentManager?.let {
            if(it.findFragmentByTag(tag) == null) {
                Log.d(TAG, "adding fragment" + fragment.toString())
                val transaction = it.beginTransaction()
                transaction.add(R.id.fragment_container, fragment, tag)
                transaction.commit()
                initAutoComplete(fragment, tag)
            } else {
                removeAutoCompleteFragment()
            }
        }
    }

    private fun removeAutoCompleteFragment() {
        activity?.supportFragmentManager?.let {
            println(it.fragments)
            val fragment = if (it.findFragmentByTag("to") != null) it.findFragmentByTag("to") else it.findFragmentByTag("from")
            if(fragment != null) {
                Log.d(TAG, "removing fragment" + fragment.toString())
                val transaction = it.beginTransaction()
                transaction.remove(fragment)
                transaction.commit()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initComponents(){
        initButtons()
        initObservables()
    }

    private fun handleFromLocation(carmenFeature: CarmenFeature) {
        fromPoint = carmenFeature.geometry() as Point
        fromLocation.text = carmenFeature.text()
        myLocation.visibility = View.INVISIBLE
        clearText.visibility = View.VISIBLE
        if(checkInputs()) next_btn.isEnabled = true
    }

    private fun handleToLocation(carmenFeature: CarmenFeature) {
        toPoint = carmenFeature.geometry() as Point
        toLocation.text = carmenFeature.text()
        if(checkInputs()) next_btn.isEnabled = true
    }

    private fun initObservables(){
        zoneViewModel.getObservableZones().observe(this, Observer { data ->
            if(data.features().isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No zones found near destination", Toast.LENGTH_SHORT).show()
            } else {
                val zone = data.features()?.first()
                if(checkInputs()) {
                    selectedZoneViewModel.selectedZone.value = zone
                    routeViewModel.destination.value = toPoint
                    routeViewModel.getWayPointRoute(origin = fromPoint, wayPoint = getGeometryPoint(zone?.geometry()), destination = toPoint )
                    progressBar.visibility = View.VISIBLE
                }
            }
        })
        routeViewModel.getRoutes().observe(this, Observer {
            if(checkInputs()) findNavController().navigate(R.id.action_tripFragment_to_mapFragment)
        })
    }

    /** Returns a point of the given geometry */
    private fun getGeometryPoint(geometry: Geometry?): Point {
        return when (geometry) {
            is Polygon -> getPolygonCenter(geometry)
            is MultiPolygon -> getMultiPolygonCenter(geometry)
            else -> geometry as Point
        }
    }

    /** Returns a middle point of a given Geometry, only used for polygons */
    private fun getPolygonCenter(geometry: Geometry): Point {
        val builder = LatLngBounds.Builder()
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

    private fun getNearestParking() {
        if (checkInputs()) zoneViewModel.getSpecificZones(toPoint.latitude(), toPoint.longitude(), 1000)
    }

    private fun initButtons() {
        toLocation.setOnClickListener { addAutoCompleteFragment(autoCompleteFragment, "to") }
        fromLocation.setOnClickListener { addAutoCompleteFragment(autoCompleteFragment, "from") }
        myLocation.setOnClickListener {
            if (fromLocation.text != getString(R.string.nuvarande_plats)) {
                fusedLocationProviderClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            location?.let {
                                fromLocation.text = getString(R.string.nuvarande_plats)
                                fromPoint = Point.fromLngLat(location.longitude, location.latitude)
                                myLocation.visibility = View.INVISIBLE
                                clearText.visibility = View.VISIBLE
                                if(checkInputs()) next_btn.isEnabled = true
                            }
                        }
            }
        }
        swapIcon.setOnClickListener {
            if (checkInputs()){
                swapLocations()
            } else {
                Toast.makeText(requireContext(), "Can´t swap", Toast.LENGTH_LONG).show()
            }
        }
        clearText.setOnClickListener {
            fromLocation.text = ""
            clearText.visibility = View.GONE
            myLocation.visibility = View.VISIBLE
            next_btn.isEnabled = false
        }
        next_btn.setOnClickListener {
            if(checkInputs()) {
                getNearestParking()
            } else Toast.makeText(requireContext(), "Choose all required alternatives", Toast.LENGTH_LONG).show()
        }
    }

    private fun swapLocations() {
        val tempText = fromLocation.text
        val tempPoint = fromPoint
        fromLocation.text = toLocation.text
        toLocation.text = tempText
        fromPoint = toPoint
        toPoint = tempPoint
    }

    private fun checkInputs(): Boolean = !toLocation.text.isNullOrEmpty() && !fromLocation.text.isNullOrEmpty()

    companion object {
        val TAG : String = "TripFragment"
    }

    override fun onPause() {
        removeAutoCompleteFragment()
        super.onPause()
    }
}
