package com.example.smspark.views

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.example.smspark.R
import kotlinx.android.synthetic.main.fragment_destination_input.*


class DestinationInputFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_destination_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView.text = arguments?.getString("fromArg")

        initButtons()
    }

    private fun initButtons() {
        next_btn.setOnClickListener {
            val bundle = Bundle()
            arguments?.putString("destinationArg", textDestination?.text.toString())

            it.findNavController().navigate(R.id.action_destinationInputFragment_to_wayPointInputFragment, arguments)
        }
    }

    companion object {
        val TAG : String = "DestinationInputFragmentFragment"
    }
}
