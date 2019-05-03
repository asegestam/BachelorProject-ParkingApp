package com.example.smspark.views


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.example.smspark.viewmodels.ZoneViewModel
import com.mapbox.geojson.Feature
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.fragment_tickets.*
import org.koin.android.viewmodel.ext.android.sharedViewModel

class TicketsFragment : Fragment() {

    //RecyclerView fields
    private lateinit var recyclerView: RecyclerView
    private lateinit var ticketAdapter: TicketAdapter

    //lazy inject ViewModel
    private val zoneViewModel: ZoneViewModel by sharedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tickets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        zoneViewModel.getObservableZones().observe(this, Observer {
            initRecyclerView(it.features()!!.toList())
        })
        zoneViewModel.getSpecificZones()
    }

    private fun initRecyclerView(features : List<Feature>){

        recyclerView = ticket_recycler_view
        ticketAdapter = TicketAdapter(features)
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerView.adapter = ticketAdapter
    }

    companion object {
        val TAG : String = "TicketsFragment"
    }
}
