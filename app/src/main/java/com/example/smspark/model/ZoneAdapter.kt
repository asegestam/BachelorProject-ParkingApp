package com.example.smspark.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import kotlin.math.round


class ZoneAdapter(context: Context, private val listener: (Feature) -> Unit): RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    private var zones: List<Feature>
    private val applicationContext: Context

    init {
        zones = emptyList()
        applicationContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {

        val cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
        return ZoneViewHolder(cardView)
    }

    override fun getItemCount(): Int = zones.size


    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        holder.bind(zones[position], listener)
    }

    fun setData(zonesData: List<Feature>) {
        this.zones = zonesData
        notifyDataSetChanged()
    }


    class ZoneViewHolder(val v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        val cardView = v.findViewById<CardView>(R.id.card_view)
        val zoneName = v.findViewById<TextView>(R.id.zoneName)
        val zoneCode = v.findViewById<TextView>(R.id.zoneCode)
        val zoneOwner = v.findViewById<TextView>(R.id.zoneOwner)
        val zoneDistance = v.findViewById<TextView>(R.id.zoneDistance)


        /** Binds the data to the viewholder by setting the text and listener */
        fun bind(zone: Feature, listner: (Feature) -> Unit) {
            zoneName.text = zone.properties.zoneName
            zoneCode.text = "Zonkod: " + zone.properties.zonecode.toString()
            zoneOwner.text = zone.properties.zoneOwner
            zoneDistance.text = round(zone.properties.distance).toString() + " m"
            v.setOnClickListener { listner(zone) }
        }


        override fun onClick(v: View?) {
        }
    }
}