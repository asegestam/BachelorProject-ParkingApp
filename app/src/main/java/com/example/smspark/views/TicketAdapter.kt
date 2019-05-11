package com.example.smspark.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.mapbox.geojson.Feature


class TicketAdapter(private val tickets: List<Feature>, private val itemClickListener: View.OnClickListener ) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ticket_list_item, parent, false)
        return TicketViewHolder(view)
    }

    override fun getItemCount() = tickets.size

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(tickets[position], itemClickListener)
    }

    class TicketViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        private val price: TextView = itemView.findViewById(R.id.ticketPrice)
        private val zoneName: TextView = itemView.findViewById(R.id.dialogZoneName)
        private val location: TextView = itemView.findViewById(R.id.location)
        private val zoneId: TextView = itemView.findViewById(R.id.zoneId)
        private val regNr: TextView = itemView.findViewById(R.id.licensplate)
        private val priceInfo: ImageView = itemView.findViewById(R.id.priceInfoBtn)
        private val date : TextView = itemView.findViewById(R.id.date)

        fun bind(feature: Feature, itemClickListener: View.OnClickListener) {
            price.text = feature.getStringProperty("price")
            date.text = feature.getStringProperty("date")
            regNr.text = "ABC123"
            zoneName.text = feature.getStringProperty("zone_name").replace(",.*".toRegex(), "")
            zoneId.text = feature.getStringProperty("zonecode")
            location.text = "Göteborg"
            priceInfo.setOnClickListener(itemClickListener)
        }

    }
}