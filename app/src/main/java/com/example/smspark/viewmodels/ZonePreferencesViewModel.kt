package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.changeValue

class ZonePreferencesViewModel: ViewModel() {

    val showAccessibleZones: MutableLiveData<Boolean> = MutableLiveData()

    val showEcsZones: MutableLiveData<Boolean> = MutableLiveData()


    init {
        showAccessibleZones.changeValue(false)
        showEcsZones.changeValue( false)
    }
}