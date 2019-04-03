package com.example.smspark.views


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.smspark.R
import kotlinx.android.synthetic.main.fragment_way_point_input.*


class WayPointInputFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_way_point_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        waypointArgs.text = "From: " + arguments?.getString("fromArg") + "\nDestination: " + arguments?.getString("destinationArg")
        initButtons()
    }

    private fun initButtons() {
        next_btn.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_wayPointInputFragment_to_carInputFragment))
    }

    companion object {
        val TAG : String = "WayPointInputFragment"
    }

}
