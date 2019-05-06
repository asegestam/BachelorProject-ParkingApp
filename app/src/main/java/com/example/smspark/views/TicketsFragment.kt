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
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.synthetic.main.current_ticket.*
import kotlinx.android.synthetic.main.fragment_tickets.*

class TicketsFragment : Fragment() {

    //RecyclerView fields
    private lateinit var recyclerView: RecyclerView
    private lateinit var ticketAdapter: TicketAdapter

    //Used to mimic if a parking is active or not
    private var parkingIsActive : Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tickets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        parkingCardView.setOnClickListener {
            if(parkingIsActive){
                parkingCardView.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimaryLight))
                parkingIsActive = false
                parkingText.text = "Inga Aktiva Parkeringar"
                Toast.makeText(context, "Parkering är ej aktiv", Toast.LENGTH_SHORT).show()
            }else{
                parkingCardView.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorSuccess))
                parkingIsActive = true
                parkingText.text = "Parkering Aktiv"
                Toast.makeText(context, "Parkering är aktiv", Toast.LENGTH_SHORT).show()
            }
        }

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
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.colorAccentLight))
        snackbar.apply {
            show()
            setAction("OK") { snackbar.dismiss() }
            setActionTextColor(ContextCompat.getColor(activity!!.applicationContext, R.color.colorPrimaryLight))
        }
    }


    companion object {
        val TAG : String = "TicketsFragment"
    }
}
