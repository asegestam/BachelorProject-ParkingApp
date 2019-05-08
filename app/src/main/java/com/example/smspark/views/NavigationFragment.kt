package com.example.smspark.views

import android.app.AlertDialog
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.smspark.R
import com.example.smspark.viewmodels.RouteViewModel
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.ui.v5.NavigationView
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlinx.android.synthetic.main.fragment_navigation.*
import org.koin.android.viewmodel.ext.android.sharedViewModel


class NavigationFragment : Fragment(), OnNavigationReadyCallback, NavigationListener, ProgressChangeListener, OffRouteListener, RouteListener {

    private lateinit var navigationView: NavigationView
    private lateinit var snackbar: Snackbar
    private var routingToDestination = false
    private val routeViewModel: RouteViewModel by sharedViewModel()
    private val selectedZoneViewModel: SelectedZoneViewModel by sharedViewModel()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_navigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity
        activity.changeNavBarVisibility(false)
        navigationView = navigation_view_fragment
        navigationView.onCreate(savedInstanceState)
        navigationView.initialize(this)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        routeViewModel.routeDestination.observe(this, Observer {
            navigationView.drawRoute(it)
            startNavigation(it)
        })
    }

    /** Starts the navigation and sets up the correct Listeners */
    private fun startNavigation(route: DirectionsRoute) {
        val options: NavigationViewOptions = NavigationViewOptions.builder()
                .directionsRoute(route)
                .shouldSimulateRoute(true)
                .navigationListener(this)
                .progressChangeListener(this)
                .routeListener(this)
                .lightThemeResId(R.style.NavigationViewLight)
                .waynameChipEnabled(true)
                .build()
        navigationView.startNavigation(options)
    }

    override fun onNavigationFinished() {
        Log.d("NavigationFragment", "Route FINISHED!!")
    }

    override fun onNavigationRunning() {
    }

    override fun onCancelNavigation() {
        findNavController().navigate(R.id.navigation_to_map)
    }

    /** Handles Progress Change along the route */
    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {

    }

    /** Shows a dialog to the user asking for confirmation to start a parking at the parking zone */
    private fun showParkingDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom)
        builder.apply {
            setTitle("Vill du starta parkering?")
            setMessage("Parkeringen kommer att startas på " + selectedZoneViewModel.selectedZone.value?.getStringProperty("zone_name"))
            setIcon(R.drawable.park_blue)
            setPositiveButton("JA") { _, _ ->
                showSnackBar(R.string.parking_success, R.color.colorSuccess)
                startWalkingDirections()
            }
            setNegativeButton("AVBRYT") { _, _ -> showSnackBar(R.string.parking_cancel, R.color.colorPrimary, Snackbar.LENGTH_LONG) }
            create()
            show()
        }
    }

    /** Starts an navigation from the parking zone to the destination as a walking route */
    private fun startWalkingDirections() {
        routingToDestination = true
        val route = routeViewModel.routeWayPoint.value!!
        navigationView.drawRoute(route)
        startNavigation(route)
    }

    /** Shows a SnackBar with the given parameters */
    private fun showSnackBar(text: Int, color: Int, length: Int = Snackbar.LENGTH_SHORT, hasButton: Boolean = false) {
        snackbar = Snackbar.make(navigation_view_fragment, text, length)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
        if (hasButton) {
            snackbar.setAction("OK") { findNavController().navigate(R.id.navigation_to_map) }
        }
        snackbar.show()
    }

    override fun onFailedReroute(errorMessage: String?) {
    }

    override fun allowRerouteFrom(offRoutePoint: Point?): Boolean {
        return true
    }

    override fun onRerouteAlong(directionsRoute: DirectionsRoute?) {
    }

    override fun onOffRoute(offRoutePoint: Point?) {
    }

    override fun onArrival() {
        if(!routingToDestination) {
            navigationView.stopNavigation()
            showParkingDialog()
        } else {
            navigationView.stopNavigation()
            showSnackBar(R.string.destination_arrival, R.color.colorAccentLight, Snackbar.LENGTH_INDEFINITE, true)
        }
        Log.d("NavigationFragment", "Arrived")
    }

    override fun userOffRoute(location: Location?) {
        location?.let {
            allowRerouteFrom(Point.fromLngLat(location.longitude, location.latitude))
        }
    }

    override fun onStart() {
        super.onStart()
        navigationView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationView.onResume()
    }

    override fun onPause() {
        super.onPause()
        navigationView.onPause()
    }

    override fun onStop() {
        super.onStop()
        navigationView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigationView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            navigationView.onRestoreInstanceState(savedInstanceState)
        }
    }
}
