package com.example.smspark.views


import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smspark.R
import com.example.smspark.model.RouteViewModel
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.ui.v5.*
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlinx.android.synthetic.main.fragment_navigation.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class NavigationFragment : Fragment(), OnNavigationReadyCallback, NavigationListener, ProgressChangeListener {
    private lateinit var navigationView: NavigationView
    private var currentRoute: DirectionsRoute? = null
    private lateinit var soundButton: NavigationButton

    private val routeViewModel: RouteViewModel by sharedViewModel { parametersOf(requireContext()) }

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
        initSoundButton()
    }

    private fun initSoundButton() {
        soundButton = navigationView.retrieveSoundButton() as SoundButton
    }

    override fun onNavigationReady(isRunning: Boolean) {
        val route = routeViewModel.route.value
        route?.let {
            currentRoute = route
            startNavigation()
        }
    }

    private fun startNavigation() {
        if(currentRoute == null) {
            return
        }
        val options: NavigationViewOptions = NavigationViewOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(true)
                .navigationListener(this)
                .progressChangeListener(this)
                .lightThemeResId(R.style.NavigationViewLight)
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

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        if(routeProgress != null) {
            val progressFraction = routeProgress.currentLegProgress().fractionTraveled()
            if(progressFraction >= 0.95f) {
                navigationView.stopNavigation()
                Toast.makeText(context, "Närmar dig parkeringen!", Toast.LENGTH_LONG).show()
            }
        }
        Log.d("NavigationFragment", "progress " + routeProgress?.currentLegProgress()?.fractionTraveled())

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
        if(savedInstanceState != null) {
            navigationView.onRestoreInstanceState(savedInstanceState)
        }
    }
}
