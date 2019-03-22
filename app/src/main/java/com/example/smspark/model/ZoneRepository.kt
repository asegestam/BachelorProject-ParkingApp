package com.example.smspark.model

import androidx.lifecycle.MutableLiveData

interface ZoneRepository {
    fun getZone(): String

    fun getZones(): MutableLiveData<String>
}