package com.example.smspark.views


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.smspark.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_profile.*


class ProfileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
    }

    private fun initButtons(){

        infoImageViewIcon.setOnClickListener {
            //TODO create popup dialog stating the cost of "pappersfaktura"
            showSnackBar("Pris för pappersfaktura är 25kr.")
        }

        paymentChangeButton.setOnClickListener {
            //TODO create UI för chaning or handling payment methods
        }

        userInfoChangeButton.setOnClickListener {
            //TODO craete UI and handle change to user information
        }
    }

    private fun showSnackBar(info: String) {
        val snackbar = Snackbar.make(profileFragment_holder, info, Snackbar.LENGTH_LONG )
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(ContextCompat.getColor(activity!!.applicationContext, R.color.colorAccentLight))
        snackbar.apply {
            show()
            setAction("OK") { snackbar.dismiss() }
            setActionTextColor(ContextCompat.getColor(activity!!.applicationContext, R.color.colorPrimaryLight))
        }
    }


//TODO Lägg till en knapp för att rensa sök historiken
    companion object {
        val TAG : String = "ProfileFragment"
    }
}
