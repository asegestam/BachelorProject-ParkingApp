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
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.ui.PlaceAutocompleteFragment
import com.mapbox.mapboxsdk.plugins.places.autocomplete.ui.PlaceSelectionListener
import kotlinx.android.synthetic.main.destination_search.*
import kotlinx.android.synthetic.main.fragment_trip.*
import org.koin.android.viewmodel.ext.android.sharedViewModel


class TripFragment : Fragment() {

    private val FROM_TEXT_VIEW = 1
    private val DESTINATION_TEXT_VIEW = 2
    private var fromLatLng: String? = null
    private var toLatLng: String? = null
    private lateinit var autoCompleteFragment: PlaceAutocompleteFragment
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    //lazy inject ViewModel
    private val zoneViewModel: ZoneViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        autoCompleteFragment = PlaceAutocompleteFragment.newInstance(getString(R.string.access_token), PlaceOptions.builder()
                .language("sv")
                .hint(getString(R.string.search_hint))
                .country("SE")
                .proximity(getUserLocation())
                .build(PlaceOptions.MODE_CARDS))
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

        toLocation.setOnClickListener { addAutoCompleteFragment(autoCompleteFragment, "to") }
        fromLocation.setOnClickListener { addAutoCompleteFragment(autoCompleteFragment, "from") }

        myLocation.setOnClickListener {
            if (fromLocation.text != getString(R.string.nuvarande_plats)) {
                fromLocation.text = getString(R.string.nuvarande_plats)
                val location = getUserLocation()
                fromLatLng = location.toJson().toString()
            }
        }

        swapIcon.setOnClickListener {
            if ((fromLatLng != null) && (toLatLng != null)){
                val tempText = fromLocation.text
                val tempPoint = fromLatLng
                fromLocation.text = toLocation.text
                toLocation.text = tempText
                fromLatLng = toLatLng
                toLatLng = tempPoint
            } else {
                Toast.makeText(requireContext(), "Can´t swap", Toast.LENGTH_LONG).show()
            }
        }
        initButtons()
        initObservables()
    }

    private fun handleFromLocation(carmenFeature: CarmenFeature) {
        fromLatLng = carmenFeature.geometry()?.toJson()
        fromLocation.text = carmenFeature.text()
    }

    private fun handleToLocation(carmenFeature: CarmenFeature) {
        toLatLng = carmenFeature.geometry()?.toJson()
        toLocation.text = carmenFeature.text()
    }

    private fun initObservables(){
        zoneViewModel.getObservableZones().observe(this, Observer { data ->
            val wayPoint: Point
            if(data.features().isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No zones found near destination", Toast.LENGTH_SHORT).show()
            } else {
                val first = data.features()?.first()
                wayPoint = if (first?.geometry() is Point)
                    first.geometry() as Point
                else {
                    val  builder  = LatLngBounds.Builder()
                    val polygon = first?.geometry() as Polygon
                    val outer = polygon.outer()
                    outer?.coordinates()?.forEach {
                        builder.include(LatLng(it.latitude(), it.longitude()))
                    }
                    val build = builder.build()
                    val center = build.center
                    Point.fromLngLat(center.longitude, center.latitude)
                }
                checkArguments(wayPoint, first)
            }
        })
    }

    private fun getNearestParking() {
        if (toLatLng != null) {
            val destinationPoint = Point.fromJson(toLatLng!!)
            zoneViewModel.getSpecificZones(destinationPoint.latitude(), destinationPoint.longitude(), 500)
        }
    }

    private fun checkArguments(wayPoint : Point, feature : Feature){
        if(fromLatLng != null && toLatLng != null){
            val bundle = Bundle()

            bundle.putString("fromArg", fromLatLng)
            bundle.putString("destArg", toLatLng)
            bundle.putString("wayPointArg", wayPoint.toJson())
            bundle.putString("wayPointFeatureArg", feature.toJson())
            //Toast.makeText(requireContext(), wayPoint.toString(), Toast.LENGTH_LONG).show()

            findNavController().navigate(R.id.action_tripFragment_to_mapFragment, bundle)
        }
    }

    private fun initButtons() {
        next_btn.setOnClickListener {
            if(fromLatLng != null && toLatLng != null)
                getNearestParking()
            else
                Toast.makeText(requireContext(), "Choose all required alternatives", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(): Point {
        var point: Point = Point.fromLngLat(57.0, 12.0)
        fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        point = Point.fromLngLat(location.longitude, location.latitude)

                    }
                }
        return point
    }


    companion object {
        val TAG : String = "TripFragment"
    }

    override fun onPause() {
        removeAutoCompleteFragment()
        super.onPause()
    }
}
