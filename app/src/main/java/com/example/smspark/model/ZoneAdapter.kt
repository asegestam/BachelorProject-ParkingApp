package com.example.smspark.model

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.example.smspark.viewmodels.ZoneViewModel
import kotlinx.android.synthetic.main.fragment_zone_list.*


class ZoneAdapter(context: Context): RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    private var zones: List<Feature>
    private val inflater: LayoutInflater

    init {
        inflater = LayoutInflater.from(context)
        zones = emptyList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {

        val cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
        return ZoneViewHolder(cardView)
    }

    override fun getItemCount(): Int = zones.size

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        Log.d("Adapter", "onBind called")
        holder.zoneName?.text = zones[position].properties.zoneName
        holder.zoneCode?.text = zones[position].properties.zonecode.toString()
        holder.zoneOwner?.text = zones[position].properties.zoneOwner
        holder.zoneDistance?.text = zones[position].properties.distance.toString() + " m"
    }

    fun setData(zonesData: List<Feature>) {
        this.zones = zonesData
        notifyDataSetChanged()
    }


    class ZoneViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        val zoneName = v.findViewById<TextView>(R.id.zoneName)
        val zoneCode = v.findViewById<TextView>(R.id.zoneCode)
        val zoneOwner = v.findViewById<TextView>(R.id.zoneOwner)
        val zoneDistance = v.findViewById<TextView>(R.id.zoneDistance)

        override fun onClick(v: View?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}