package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ZonePreferencesViewModel: ViewModel() {

    val showAccessibleZones: MutableLiveData<Boolean> = MutableLiveData()

    val showEcsZones: MutableLiveData<Boolean> = MutableLiveData()


    init {
        showAccessibleZones.value = false
        showEcsZones.value = false
    }
}