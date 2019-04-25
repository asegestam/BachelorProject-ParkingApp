package com.example.smspark.dimodules

import com.example.smspark.model.routemodel.RouteRepository
import com.example.smspark.viewmodels.RouteViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val routeModule = module {
    single { RouteRepository(androidContext()) }
    viewModel { RouteViewModel(get()) }
}