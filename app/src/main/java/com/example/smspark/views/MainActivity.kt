package com.example.smspark.views

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController

import com.example.smspark.R
import kotlinx.android.synthetic.main.activity_main.*


@SuppressLint("LogNotTimber")
class MainActivity : AppCompatActivity() {

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

    companion object {
        private val TAG = "MainActivity"
    }
}
