package com.example.smspark.model

import androidx.lifecycle.LiveData

interface ZoneRepository {

    fun getZones() : LiveData<Zone>

    fun getSpecificZones(latitude: Double, longitude: Double, radius: Int): LiveData<Zone>

    fun getHandicapZones(): LiveData<String>
}