package com.example.smspark.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.smspark.R
import com.example.smspark.model.changeValue
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import kotlinx.android.synthetic.main.activity_main.*


@SuppressLint("LogNotTimber")
class MainActivity : AppCompatActivity(), PermissionsListener {

    private val permissionsManager: PermissionsManager by lazy { PermissionsManager(this) }
    val locationPermissionGranted: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navController = findNavController(R.id.nav_host_fragment)
        navigationView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bottom_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    fun changeNavBarVisibility(visible: Boolean) {
        when(visible) {
            true -> navigationView.visibility = View.VISIBLE
            false -> navigationView.visibility = View.GONE
        }
    }

    fun requestLocationPermission() {
        permissionsManager.requestLocationPermissions(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Log.d("onPermissionResult", "permission granted")
            locationPermissionGranted.changeValue(true)
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show()
            permissionsManager.requestLocationPermissions(this)
        }
    }


    companion object {
        private const val TAG = "MainActivity"
    }
}
