package com.example.smspark.views


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.smspark.R
import kotlinx.android.synthetic.main.fragment_car_input.*


class CarInputFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_car_input, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initButtons()
    }

    private fun initButtons() {
        //next_btn.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_tripFragment_to_destinationInputFragment))
    }

    companion object {
        val TAG : String = "CarInputFragment"
    }

}
