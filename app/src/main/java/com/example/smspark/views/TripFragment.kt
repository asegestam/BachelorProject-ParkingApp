package com.example.smspark.views


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.smspark.R
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import kotlinx.android.synthetic.main.fragment_trip.*
import org.koin.android.viewmodel.ext.android.sharedViewModel


class TripFragment : Fragment(), OnMapReadyCallback {

    val FROM_TEXT_VIEW = 1
    val DESTINATION_TEXT_VIEW = 2
    var fromLatLng: String? = null
    var destinationLatLng: String? = null

    //lazy inject ViewModel
    private val zoneViewModel: ZoneViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initComponents()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {}

    @SuppressLint("MissingPermission")
    private fun initComponents(){
        textFrom.setOnClickListener { startAutoCompleteActivity(FROM_TEXT_VIEW) }
        textDestination.setOnClickListener { startAutoCompleteActivity(DESTINATION_TEXT_VIEW) }
        iv_my_location.setOnClickListener {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if ((location != null) && (textDestination.text.toString() != "Current Location")) {
                            textFrom.text = "Current Location"
                            var point = Point.fromLngLat(location.longitude, location.latitude)
                            fromLatLng = point.toJson().toString()
                        }
                    }
        }

        iv_swap.setOnClickListener {
            if ((fromLatLng != null) && (destinationLatLng != null)){
                val tempText = textFrom.text.toString()
                val tempPoint = fromLatLng
                textFrom.text = textDestination.text
                textDestination.text = tempText
                fromLatLng = destinationLatLng
                destinationLatLng = tempPoint
            } else {
                Toast.makeText(requireContext(), "Can´t swap", Toast.LENGTH_LONG).show()
            }
        }

        initButtons()
        initSpinner()
        initObservables()

    }

    private fun initObservables(){
        zoneViewModel.getObservableZones().observe(this, Observer { data ->
            var wayPoint: Point? = null
            val first = data.features()?.first()

            //Toast.makeText(requireContext(), "" +first?.getNumberProperty("distance"), Toast.LENGTH_LONG ).show()
            first?.let {
                if (first.geometry() is Point)
                    wayPoint = first.geometry() as Point
                else {
                    val  builder  = LatLngBounds.Builder()
                    val polygon = first.geometry() as Polygon
                    val outer = polygon.outer()
                    outer?.coordinates()?.forEach {
                        builder.include(LatLng(it.latitude(), it.longitude()))
                    }
                    val build = builder.build()
                    val center = build.center
                    wayPoint = Point.fromLngLat(center.longitude, center.latitude)
                }
            }
            wayPoint.let { checkArguments(wayPoint!!, first!!) }
        })
    }

    private fun initSpinner(){
        var arrayAdapter = ArrayAdapter<String>(requireContext(),
                R.layout.support_simple_spinner_dropdown_item,
                resources.getStringArray(R.array.vehicles))

        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)

        spinner.adapter = arrayAdapter
    }

    /** Starts a Search AutoComplete activity for searching locations */
    private fun startAutoCompleteActivity(id: Int) {
        val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(getString(R.string.access_token))
                .placeOptions(PlaceOptions.builder()
                        .language("sv")
                        .country("SE")
                        .proximity(Point.fromLngLat(11.9745, 57.7088))
                        .build(PlaceOptions.MODE_CARDS))
                .build(requireActivity())
        startActivityForResult(intent, id)
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //if result code is for AutoComplete activity
        if (resultCode == Activity.RESULT_OK && (requestCode == FROM_TEXT_VIEW || requestCode == DESTINATION_TEXT_VIEW)) {
            handleAutoCompleteResult(data, requestCode)
        }
    }

    /** Handles the result given from the Search AutoComplete Activity*/
    private fun handleAutoCompleteResult(data: Intent?, id: Int) {
        //Gets the place data from searched position
        val feature = PlaceAutocomplete.getPlace(data)
        val point = feature?.geometry() as Point

        when(id){
            FROM_TEXT_VIEW -> {
                textFrom.text = feature.placeName()
                fromLatLng = point.toJson().toString()
            }
            DESTINATION_TEXT_VIEW -> {
                textDestination.text = feature.placeName()
                destinationLatLng = point.toJson().toString()
            }
        }

        //Toast.makeText(requireContext(), point.toString(), Toast.LENGTH_LONG).show()
    }

    private fun getNearestParking() {
        if (destinationLatLng != null) {
            val destinationPoint = Point.fromJson(destinationLatLng!!)
            zoneViewModel.getSpecificZones(destinationPoint.latitude(), destinationPoint.longitude(), 500)
        }
    }

    private fun checkArguments(wayPoint : Point, feature : Feature){
        if(fromLatLng != null && destinationLatLng != null){
            val bundle = Bundle()

            bundle.putString("fromArg", fromLatLng)
            bundle.putString("destArg", destinationLatLng)
            bundle.putString("wayPointArg", wayPoint.toJson())
            bundle.putString("wayPointFeatureArg", feature.toJson())
            //Toast.makeText(requireContext(), wayPoint.toString(), Toast.LENGTH_LONG).show()

            findNavController().navigate(R.id.action_tripFragment_to_mapFragment, bundle)
        }
    }

    private fun initButtons() {
        next_btn.setOnClickListener {
            if(fromLatLng != null && destinationLatLng != null)
                getNearestParking()
            else
                Toast.makeText(requireContext(), "Choose all required alternatives", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        val TAG : String = "TripFragment"
    }
}
