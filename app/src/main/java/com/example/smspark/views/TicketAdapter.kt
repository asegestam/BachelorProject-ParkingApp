package com.example.smspark.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R

class TicketAdapter(val tickets: ArrayList<String>) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketAdapter.TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ticket_list_item, parent, false)
        return TicketViewHolder(view)
    }

    override fun getItemCount() = tickets.size

    override fun onBindViewHolder(holder: TicketAdapter.TicketViewHolder, position: Int) {

    }

    class TicketViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

    }
}