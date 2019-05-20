package com.example.smspark.dimodules

import com.example.smspark.viewmodels.TicketViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val ticketModule = module {
    viewModel { TicketViewModel() }
}