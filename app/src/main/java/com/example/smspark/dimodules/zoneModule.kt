package com.example.smspark.dimodules

import com.example.smspark.model.ZoneRepository
import com.example.smspark.model.ZoneRepositoryImpl
import com.example.smspark.viewmodels.ZoneViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val zoneModule = module {

    //Single instance of ZoneRepository
    single<ZoneRepository> { ZoneRepositoryImpl() }

    //MyViewModel ViewModel
    viewModel { ZoneViewModel(get()) }
}
