package com.example.smspark.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.google.android.material.button.MaterialButton
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection

class ZoneAdapter(private val listener: (Feature) -> Unit, private val itemClickListener: View.OnClickListener): RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    private lateinit var zones: FeatureCollection
    private val featureList: ArrayList<Feature> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
        return ZoneViewHolder(cardView, itemClickListener)
    }

    override fun getItemCount(): Int = featureList.size


    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        holder.bind(zones.features()!![position], listener)
    }

    fun setData(zonesData: List<Feature>) {
        featureList.clear()
        zonesData.forEach {
            if(!featureList.contains(it)) featureList.add(it)
        }
        featureList.sortBy { it.getNumberProperty("distance").toInt()}
        val featureCollection = FeatureCollection.fromFeatures(featureList)
        zones = featureCollection
        notifyDataSetChanged()
    }

    class ZoneViewHolder(private val v: View, private val itemClickListener: View.OnClickListener) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private val icon: ImageView = v.findViewById(R.id.icon)
        private val zoneName: TextView = v.findViewById(R.id.zoneName)
        private val zoneType: TextView = v.findViewById(R.id.zoneType)
        private val zoneOwner: TextView = v.findViewById(R.id.zoneOwner)
        private val zoneDistance: TextView = v.findViewById(R.id.zoneDistance)
        private val hideButton: MaterialButton = v.findViewById(R.id.hide_list_button)


        /** Binds the data to the viewholder by setting the text and listener */
        fun bind(zone: Feature, listner: (Feature) -> Unit) {
            if(zone.hasProperty("wkt")) {
                zoneType.text = "Typ: HandikappsParkering"
                icon.setImageResource(R.drawable.accessible_png)
            }
            else {
                icon.setImageResource(R.drawable.park_blue)
                zoneType.text = "Zonkod: " + zone.getNumberProperty("zonecode")
            }
                zoneName.text = zone.getStringProperty("zone_name")
                zoneOwner.text = zone.getStringProperty("zone_owner")
                zoneDistance.text = zone.getNumberProperty("distance").toInt().toString() + " m"
            v.setOnClickListener { listner(zone) }
            hideButton.setOnClickListener(itemClickListener)
        }

        override fun onClick(v: View?) {
        }
    }
}
