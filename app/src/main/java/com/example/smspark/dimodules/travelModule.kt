package com.example.smspark.dimodules

import com.example.smspark.model.travelmodel.TravelCalc
import com.example.smspark.model.travelmodel.TravelCalcImpl
import com.example.smspark.viewmodels.TravelViewModel
import com.example.smspark.viewmodels.ZoneViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val travelModule = module {

    //Single instance of ZoneRepository
    single<TravelCalc> { TravelCalcImpl() }

    viewModel { TravelViewModel(get()) }

}