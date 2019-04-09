package com.example.smspark.model.ZoneModel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlin.math.round


class ZoneAdapter(context: Context, private val listener: (com.mapbox.geojson.Feature) -> Unit): RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    private lateinit var zones: FeatureCollection
    private val featureList: ArrayList<Feature> = ArrayList()
    private val applicationContext: Context

    init {
        applicationContext = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
        return ZoneViewHolder(cardView)
    }

    override fun getItemCount(): Int = featureList.size


    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        holder.bind(zones.features()!![position], listener)
    }

    fun setData(zonesData: FeatureCollection) {
        zonesData.features()?.forEach {
            if(!featureList.contains(it)) featureList.add(it)
        }
        val featureCollection = FeatureCollection.fromFeatures(featureList)
        zones = featureCollection
        notifyDataSetChanged()
    }


    class ZoneViewHolder(val v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        val cardView = v.findViewById<CardView>(R.id.card_view)
        val icon = v.findViewById<ImageView>(R.id.icon)
        val zoneName = v.findViewById<TextView>(R.id.zoneName)
        val zoneType = v.findViewById<TextView>(R.id.zoneType)
        val zoneOwner = v.findViewById<TextView>(R.id.zoneOwner)
        val zoneDistance = v.findViewById<TextView>(R.id.zoneDistance)


        /** Binds the data to the viewholder by setting the text and listener */
        fun bind(zone: com.mapbox.geojson.Feature, listner: (Feature) -> Unit) {
            if(zone.hasProperty("zone_owner")) {
                zoneName.text = zone.getStringProperty("zone_name")
                zoneType.text = "Zonkod: " + zone.getNumberProperty("zonecode")
                zoneOwner.text = zone.getStringProperty("zone_owner")
                zoneDistance.text = zone.getNumberProperty("distance").toInt().toString() + " m"
            } else {
                icon.setImageResource(R.drawable.handicap_icon)
                zoneName.text = zone.getStringProperty("Name")
                zoneType.text = "Typ: HandikappsParkering"
                zoneOwner.text = zone.getStringProperty("Owner")
                zoneDistance.text = zone.getNumberProperty("Distance").toInt().toString() + " m"
            }
            v.setOnClickListener { listner(zone) }
        }


        override fun onClick(v: View?) {
        }
    }
}
