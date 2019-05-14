package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Feature

class TicketViewModel: ViewModel() {

    val activeParking: MutableLiveData<Pair<Boolean, Feature>> by lazy {
        MutableLiveData<Pair<Boolean, Feature>>()
    }
}