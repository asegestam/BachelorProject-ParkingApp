package com.example.smspark.views


import android.annotation.SuppressLint
import android.location.Location
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
import com.example.smspark.R
import com.example.smspark.model.GeometryUtils
import com.example.smspark.viewmodels.RouteViewModel
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.ui.PlaceAutocompleteFragment
import com.mapbox.mapboxsdk.plugins.places.autocomplete.ui.PlaceSelectionListener
import kotlinx.android.synthetic.main.destination_search.*
import kotlinx.android.synthetic.main.fragment_tickets.*
import kotlinx.android.synthetic.main.fragment_trip.*
import kotlinx.android.synthetic.main.trip_options.*
import org.koin.android.viewmodel.ext.android.sharedViewModel

@SuppressLint("MissingPermission")
class TripFragment : Fragment() {

    private lateinit var fromPoint: Point
    private lateinit var toPoint: Point
    private lateinit var autoCompleteFragment: PlaceAutocompleteFragment
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var distance: Int = 500
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
        distanceText.text = "$distance m"
        initComponents()
    }

    /** Adds a place selected listener to a given PlaceAutoCompleteFragment */
    private fun addPlaceSelectionListener(fragment: PlaceAutocompleteFragment, tag: String) {
        fragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(carmenFeature: CarmenFeature) {
                when (tag) {
                    "to" -> handleToLocation(carmenFeature)
                    "from" -> handleFromLocation(carmenFeature)
                }
                //remove fragment when it has done it's job
                removeAutoCompleteFragment()
            }

            override fun onCancel() {
                //remove fragment when user cancels the widget
                removeAutoCompleteFragment()
            }
        })
    }

    /** Commit a fragment transaction of a PlaceAutocompleteFragment */
    private fun addAutoCompleteFragment(fragment: PlaceAutocompleteFragment, tag: String) {
        activity?.supportFragmentManager?.let {
            // check if there is a active AutoCompleteFragment, remove it
            if (getActiveAutoCompleteFragment() != null) {
                removeAutoCompleteFragment()
            } else {
                //no active AutoCompleteFragment, add the given one
                Log.d(TAG, "adding fragment" + fragment.toString())
                val transaction = it.beginTransaction()
                transaction.add(R.id.fragment_container, fragment, tag)
                transaction.commit()
                addPlaceSelectionListener(fragment, tag)
            }
        }
    }

    /** Commit a fragment removal transaction of a PlaceAutocompleteFragment */
    private fun removeAutoCompleteFragment() {
        activity?.supportFragmentManager?.let {
            val fragment = getActiveAutoCompleteFragment()
            if (fragment != null) {
                Log.d(TAG, "removing fragment" + fragment.toString())
                val transaction = it.beginTransaction()
                transaction.remove(fragment)
                transaction.commit()
            }
        }
    }

    /** Tries to return an active AutoCompleteFragment, if there is none return null */
    private fun getActiveAutoCompleteFragment(): Fragment? {
        val manager = activity?.supportFragmentManager
        val toFragment = manager?.findFragmentByTag("to")
        val fromFragment = manager?.findFragmentByTag("from")
        return toFragment ?: fromFragment
    }

    @SuppressLint("MissingPermission")
    private fun initComponents() {
        initButtons()
        initObservables()
    }

    /** Handles the from location part of the search
     * changes the text and showing the correct views
     * */
    private fun handleFromLocation(carmenFeature: CarmenFeature) {
        fromPoint = carmenFeature.geometry() as Point
        fromLocation.text = carmenFeature.text()
        myLocation.visibility = View.INVISIBLE
        clearText.visibility = View.VISIBLE
        if (checkInputs()) next_btn.visibility = View.VISIBLE
    }

    /** Handles the to location part of the search
     * changes the text
     * */
    private fun handleToLocation(carmenFeature: CarmenFeature) {
        toPoint = carmenFeature.geometry() as Point
        toLocation.text = carmenFeature.text()
        if (checkInputs()) next_btn.visibility = View.VISIBLE
    }

    private fun initObservables() {
        zoneViewModel.getAllZones().observe(this, Observer { hashMap ->
            progressBar.visibility = View.GONE
            hashMap["standard"]?.features()?.let {
                if(!it.isNullOrEmpty()){
                    val zone = it.first()
                    if (checkInputs()) {
                        selectZoneGetRoute(zone)
                    }
                } else showNoZoneFound()
            }
        })
        routeViewModel.routeMap.observe(this, Observer {
            if (it.count() >= 2 && checkInputs()) {
                findNavController().navigate(R.id.action_tripFragment_to_mapFragment)
            }
        })
    }

    private fun showNoZoneFound() {
        val snackbar = Snackbar.make(tripFragmentContent, "Inga parkeringar hittades \nTesta att öka max avståndet!", Snackbar.LENGTH_LONG )
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.colorAccentLight))
        snackbar.apply {
            setAction("OK") {
                optionsCardView.performClick()
                snackbar.dismiss()
            }
            setActionTextColor(ContextCompat.getColor(activity!!.applicationContext, R.color.colorPrimaryLight))
            show()
        }
    }

    private fun selectZoneGetRoute(zone: Feature?) {
        zone?.let {
            selectedZoneViewModel.selectedZone.value = zone
        }
        routeViewModel.destination.value = toPoint
        routeViewModel.getWayPointRoute(origin = fromPoint, wayPoint = geometryUtils.getGeometryPoint(zone?.geometry()), destination = toPoint)
        progressBar.visibility = View.VISIBLE
    }

    private fun getZones() {
        if (checkInputs()) {
            progressBar.visibility = View.VISIBLE
            zoneViewModel.getSpecificZones(toPoint.latitude(), toPoint.longitude(), distance)
        }
    }

    /** Initiates the button click listeners */
    private fun initButtons() {
        initSearchBar()
        initSeekBar()
        initSwitches()
        next_btn.setOnClickListener {
            if (checkInputs()) {
                getZones()
            } else Toast.makeText(requireContext(), "Choose all required alternatives", Toast.LENGTH_LONG).show()
        }
    }

    private fun initSearchBar() {
        toLocation.setOnClickListener { addAutoCompleteFragment(autoCompleteFragment, "to") }
        fromLocation.setOnClickListener { addAutoCompleteFragment(autoCompleteFragment, "from") }
        myLocation.setOnClickListener {
            if (fromLocation.text != getString(R.string.nuvarande_plats)) {
                setUserLocation()
            }
        }
        swapIcon.setOnClickListener {
            if (checkInputs()) swapLocations()
            else Toast.makeText(requireContext(), "Can´t swap", Toast.LENGTH_LONG).show()
        }
        clearText.setOnClickListener {
            fromLocation.text = ""
            clearText.visibility = View.GONE
            myLocation.visibility = View.VISIBLE
            next_btn.visibility = View.GONE
        }
    }

    private fun setUserLocation() {
        fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        fromLocation.text = getString(R.string.nuvarande_plats)
                        fromPoint = Point.fromLngLat(location.longitude, location.latitude)
                        myLocation.visibility = View.INVISIBLE
                        clearText.visibility = View.VISIBLE
                        if (checkInputs()) next_btn.visibility = View.VISIBLE
                    }
                }
    }

    private fun initSeekBar() {
        rangeSeekBar.setOnRangeChangedListener(object : OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar, leftValue: Float, rightValue: Float, isFromUser: Boolean) {
                rangeSeekBar.setIndicatorText(leftValue.toInt().toString() + "")
                distance = leftValue.toInt()
            }

            override fun onStartTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {
                distanceText.visibility = View.GONE
            }

            override fun onStopTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {
                distanceText.text = "$distance m"
                distanceText.visibility = View.VISIBLE
            }
        })
    }

    private fun initSwitches() {
        accessibleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            when(isChecked){
                //TODO sätt nån variabel i en viewmodel
            }
        }
        ecsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            when(isChecked){
                //TODO sätt nån variabel i en viewmodel
            }
        }
        priceSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            when(isChecked){
                //TODO sätt nån variabel i en viewmodel
            }
        }
    }

    /** Swaps the content of the textviews
     * fromLocation text becomes toLocation text and vice versa*/
    private fun swapLocations() {
        val tempText = fromLocation.text
        val tempPoint = fromPoint
        fromLocation.text = toLocation.text
        toLocation.text = tempText
        fromPoint = toPoint
        toPoint = tempPoint
    }

    /** Checks if the TextViews has inputs */
    private fun checkInputs(): Boolean = !toLocation.text.isNullOrEmpty() && !fromLocation.text.isNullOrEmpty()

    companion object {
        val TAG: String = "TripFragment"
        val geometryUtils = GeometryUtils()
    }

    override fun onPause() {
        removeAutoCompleteFragment()
        super.onPause()
    }
}
