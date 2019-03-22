package com.example.smspark.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smspark.model.ZoneRepository

class ZoneViewModel(val repo: ZoneRepository): ViewModel(){

    val zones:  MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun printZone() = "${repo.getZone()} from $this"

    fun getZones() {
        val data = repo.getZones().value
        if(data != null) {
            zones.value = data
        }
    }

}