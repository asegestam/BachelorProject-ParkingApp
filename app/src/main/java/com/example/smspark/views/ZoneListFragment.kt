package com.example.smspark.views


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.smspark.R
import com.example.smspark.model.Feature
import com.example.smspark.model.Zone
import com.example.smspark.model.ZoneAdapter
import com.example.smspark.viewmodels.ZoneViewModel
import kotlinx.android.synthetic.main.fragment_zone_list.*
import org.koin.android.viewmodel.ext.android.viewModel


class ZoneListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var zoneAdapter: RecyclerView.Adapter<*>
    private lateinit var linearLayoutManager: LinearLayoutManager

    //lazy inject ViewModel
    val zoneViewModel: ZoneViewModel by viewModel()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_zone_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = recycler_view
        zoneViewModel.getZones()
        zoneViewModel.zoneFeatures.value = emptyList()
        zoneAdapter = ZoneAdapter(zoneViewModel.zoneFeatures.value!!)
        recyclerView.adapter = zoneAdapter
        recyclerView.layoutManager = LinearLayoutManager(activity)

        zoneViewModel.zoneFeatures.observe(this, Observer {
            features -> zoneAdapter.notifyDataSetChanged()
        })
    }


    override fun onDetach() {
        super.onDetach()
    }

}
