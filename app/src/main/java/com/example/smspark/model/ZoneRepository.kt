package com.example.smspark.model

import androidx.lifecycle.LiveData

interface ZoneRepository {

    fun getZones(): LiveData<HashMap<String, String>>
}