package com.example.smspark.views

import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.mapbox.geojson.Feature
import java.util.*
import kotlin.random.Random

class TicketAdapter(val tickets: List<Feature>) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketAdapter.TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ticket_list_item, parent, false)
        return TicketViewHolder(view)
    }

    override fun getItemCount() = tickets.size

    override fun onBindViewHolder(holder: TicketAdapter.TicketViewHolder, position: Int) {
        holder.price.text = (1..95).shuffled().first().toString() + "kr"
        holder.regNr.text = "ABC123"
        holder.zoneName.text = tickets[position].getStringProperty("zone_name").replace(",.*".toRegex(), "")
        holder.zoneId.text = tickets[position].getStringProperty("zonecode")
        holder.location.text = "Göteborg"
    }

    class TicketViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val price: TextView = itemView.findViewById(R.id.ticketPrice)
        val zoneName: TextView = itemView.findViewById(R.id.zoneName)
        val location: TextView = itemView.findViewById(R.id.location)
        val zoneId: TextView = itemView.findViewById(R.id.zoneId)
        val regNr: TextView = itemView.findViewById(R.id.licensplate)
    }
}