package com.example.smspark.dimodules

import com.example.smspark.model.zonemodel.ZoneRepository
import com.example.smspark.model.zonemodel.ZoneRepositoryImpl
import com.example.smspark.viewmodels.SelectedZoneViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val zoneModule = module {

    //Single instance of ZoneRepository
    single<ZoneRepository> { ZoneRepositoryImpl() }

    //ZoneViewModel ViewModel
    viewModel { ZoneViewModel(get()) }
    //SelectedZoneViewModel ViewModel
    viewModel { SelectedZoneViewModel(get()) }

}
