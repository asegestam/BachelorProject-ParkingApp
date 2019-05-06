package com.example.smspark.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.mapbox.geojson.Feature


class TicketAdapter(val tickets: List<Feature>, private val itemClickListener: View.OnClickListener ) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketAdapter.TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ticket_list_item, parent, false)
        return TicketViewHolder(view)
    }

    override fun getItemCount() = tickets.size

    override fun onBindViewHolder(holder: TicketAdapter.TicketViewHolder, position: Int) {
        holder.bind(tickets[position], itemClickListener)
    }

    class TicketViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val price: TextView = itemView.findViewById(R.id.ticketPrice)
        val zoneName: TextView = itemView.findViewById(R.id.zoneName)
        val location: TextView = itemView.findViewById(R.id.location)
        val zoneId: TextView = itemView.findViewById(R.id.zoneId)
        val regNr: TextView = itemView.findViewById(R.id.licensplate)
        val priceInfo: ImageView = itemView.findViewById(R.id.priceInfoBtn)

        fun bind(feature: Feature, itemClickListener: View.OnClickListener) {
            price.text = (1..95).shuffled().first().toString() + "kr"
            regNr.text = "ABC123"
            zoneName.text = feature.getStringProperty("zone_name").replace(",.*".toRegex(), "")
            zoneId.text = feature.getStringProperty("zonecode")
            location.text = "Göteborg"
            priceInfo.setOnClickListener(itemClickListener)
        }

    }
}