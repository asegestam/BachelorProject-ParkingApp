package com.example.smspark.views


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.findNavController
import com.example.smspark.R
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import kotlinx.android.synthetic.main.fragment_trip.*


class TripFragment : Fragment(), OnMapReadyCallback {

    val FROM_TEXT_VIEW = 1
    val DESTINATION_TEXT_VIEW = 2
    var fromLatLng: String? = null
    var destinationLatLng: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
        initSpinner()
        initTextViews()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {}

    private fun initTextViews(){
        textFrom.setOnClickListener { startAutoCompleteActivity(FROM_TEXT_VIEW) }
        textDestination.setOnClickListener { startAutoCompleteActivity(DESTINATION_TEXT_VIEW) }

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
                textFrom.text = feature?.placeName()
                fromLatLng = point.toJson().toString()
            }
            DESTINATION_TEXT_VIEW -> {
                textDestination.text = feature?.placeName()
                destinationLatLng = point.toJson().toString()
            }
        }

        Toast.makeText(requireContext(), point.toString(), Toast.LENGTH_LONG).show()
    }


    private fun initButtons() {
        next_btn.setOnClickListener {
            val bundle = Bundle()

            bundle.putString("fromArg", fromLatLng)
            bundle.putString("destArg", destinationLatLng)

            it.findNavController().navigate(R.id.action_tripFragment_to_mapFragment, bundle)
        }
    }



    companion object {
        val TAG : String = "TripFragment"
    }
}
