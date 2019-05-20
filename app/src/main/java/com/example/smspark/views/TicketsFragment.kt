package com.example.smspark.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smspark.R
import com.example.smspark.model.extentionFunctions.changeVisibility
import com.example.smspark.viewmodels.TicketViewModel
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.synthetic.main.active_ticket_cardview.*
import kotlinx.android.synthetic.main.fragment_tickets.*
import org.koin.android.viewmodel.ext.android.sharedViewModel

class TicketsFragment : Fragment() {

    //RecyclerView fields
    private lateinit var recyclerView: RecyclerView
    private lateinit var ticketAdapter: TicketAdapter
    private val ticketViewModel: TicketViewModel by sharedViewModel()

    //Used to mimic if a parking is active or not
    private var parkingIsActive : Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tickets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showNavbar()
        initRecyclerView()
        initObservables()
        initClicklisteners()
    }

    private fun showNavbar(){
        val activity = activity as MainActivity
        activity.changeNavBarVisibility(true)
    }

    private fun initObservables(){
        ticketViewModel.activeParking.observe(this, androidx.lifecycle.Observer {
            if (it.first)
                activateParkingCard(it.second)
            else
                deactivateParkingCard()
        })
    }

    private fun initClicklisteners(){
        parkingCardView.setOnClickListener {
            if(parkingIsActive){
                parkingCardView.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimaryLight))
                parkingIsActive = false
                Toast.makeText(context, "Parkering är ej aktiv", Toast.LENGTH_SHORT).show()
            }else{
                parkingCardView.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorSuccess))
                parkingIsActive = true
                Toast.makeText(context, "Parkering är aktiv", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun activateParkingCard(feature: Feature){
        //TODO make a new layout for the cardview and update it here for the user
        parkingCardView.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorSuccess))
        val zoneCode = feature.getStringProperty("zonecode")?.toInt() ?: 999
        activeTicketZoneName.text = feature.getStringProperty("zone_name")
        activeTicketZoneCode.text = zoneCode.toString()
        ticketStartingTime.text = feature.getStringProperty("parking_time_started").replace(":(?<=:)[^:]*\$".toRegex(), "")
        parkingCardView.changeVisibility(View.VISIBLE)

    }

    private fun deactivateParkingCard(){
        //TODO when the new layout is made create a view which says "Ingen parkering Aktiv" and add the old parking to the recyclerview for old parking tickets
        parkingCardView.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimaryLight))
    }

    private fun returnFeatureCollection() : FeatureCollection {
        return FeatureCollection.fromJson(getString(R.string.fake_parking_tickets))
    }

    private fun initRecyclerView(){
        recyclerView = ticket_recycler_view
        ticketAdapter = TicketAdapter(returnFeatureCollection().features()!!, itemClickListener = View.OnClickListener { showSnackBar() })
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerView.adapter = ticketAdapter
    }

    private fun showSnackBar() {
        val snackbar = Snackbar.make(ticketFragment_holder, "Denna parkering faktureras på nästkommande faktura", Snackbar.LENGTH_LONG )
        snackbar.apply {
            show()
            setAction("OK") { snackbar.dismiss() }
        }
    }


    companion object {
        const val TAG : String = "TicketsFragment"
    }
}
