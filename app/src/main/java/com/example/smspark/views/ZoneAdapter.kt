package com.example.smspark.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.mapbox.geojson.Feature

class ZoneAdapter(private val listener: (Feature) -> Unit): RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    private var zones: ArrayList<Feature> = ArrayList()
    private val featureList: ArrayList<Feature> = ArrayList()
    private val accessibleFeatureList: ArrayList<Feature> = ArrayList()
    private var allFeatures: ArrayList<Feature> = ArrayList()

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
        featureList.clear()
        zonesData.forEach { featureList.add(it) }
        combineAndNotify()
    }

    fun setAccessibleData(zonesData: List<Feature>) {
        accessibleFeatureList.clear()
        zonesData.forEach {
            if(!accessibleFeatureList.contains(it)) accessibleFeatureList.add(it)
        }
        combineAndNotify()
    }

    /** Combines the two feature lists */
    private fun combineAndNotify() {
        allFeatures.clear()
        allFeatures.addAll(featureList)
        allFeatures.addAll(accessibleFeatureList)
        allFeatures.sortBy { it.getNumberProperty("distance").toInt()}
        zones = allFeatures
        notifyDataSetChanged()
    }

    fun removeZonesFromList(layerID: String) {
        if(layerID == "zone-polygons-layer") removeStandardZonesFromList()
        else removeAccessibleZonesFromList()
    }

    fun addZonesToList(layerID: String) {
        if(layerID == "zone-polygons-layer") addStandardZonesToList()
        else addAccessibleZonesToList()
    }

    /** Changes the data set to all features that is a regular zone */
    private fun removeStandardZonesFromList() {
        allFeatures.removeAll{!it.hasProperty("wkt")}
        zones = allFeatures
        notifyDataSetChanged()
    }

    /** Changes the data set to all features that is not a Göteborg stad zone */
    fun removeAccessibleZonesFromList() {
        allFeatures.removeAll { it.hasProperty("wkt") }
        zones = allFeatures
        notifyDataSetChanged()
    }

    private fun addStandardZonesToList() {
        allFeatures.addAll(featureList)
        allFeatures.sortBy { it.getNumberProperty("distance").toInt() }
        zones = allFeatures
        notifyDataSetChanged()
    }

    private fun addAccessibleZonesToList() {
        allFeatures.removeAll { it.hasProperty("wkt") }
        allFeatures.addAll(accessibleFeatureList)
        allFeatures.sortBy { it.getNumberProperty("distance").toInt() }
        zones = allFeatures
        notifyDataSetChanged()
    }

    fun clearAccessibleZones() {
        accessibleFeatureList.clear()
        allFeatures.removeAll { it.hasProperty("wkt") }
        notifyDataSetChanged()
    }

    fun isAccessibleZonesEmpty(): Boolean = accessibleFeatureList.isEmpty()

    class ZoneViewHolder(private val cardView: View) : RecyclerView.ViewHolder(cardView), View.OnClickListener {

        private val icon: ImageView = cardView.findViewById(R.id.icon)
        private val zoneName: TextView = cardView.findViewById(R.id.zoneName)
        private val zoneDistance: TextView = cardView.findViewById(R.id.zoneDistance)

        /** Binds the data to the viewholder by setting the text and listener */
        fun bind(zone: Feature, listner: (Feature) -> Unit) {
            if(zone.hasProperty("wkt")) {
                icon.setImageResource(R.drawable.accessible_png)
            }
            else {
                icon.setImageResource(R.drawable.park_blue)
            }
                zoneName.text = zone.getStringProperty("zone_name")
                zoneDistance.text = zone.getNumberProperty("distance").toInt().toString() + " m"
            cardView.setOnClickListener { listner(zone) }
        }

        override fun onClick(v: View?) {
        }
    }
}
