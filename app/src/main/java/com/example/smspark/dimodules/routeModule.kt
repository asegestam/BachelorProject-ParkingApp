package com.example.smspark.dimodules

import android.content.Context
import com.example.smspark.model.RouteViewModel
import org.koin.dsl.module

val routeModule = module {
    single { (context: Context) -> RouteViewModel(context)}
}