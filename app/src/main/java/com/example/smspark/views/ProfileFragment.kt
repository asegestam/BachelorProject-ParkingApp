package com.example.smspark.views


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.smspark.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.user_info_edit.view.*


class ProfileFragment : Fragment() {



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
        val sharedPreferences = activity?.getSharedPreferences("UserInfoPreferences", Context.MODE_PRIVATE) ?: return
        val phoneNumber = sharedPreferences.getString("phone_nr", getString(R.string.default_phoneNr))
        val userEmail = sharedPreferences.getString("email", getString(R.string.default_email))
        val userAddress = sharedPreferences.getString("address", getString(R.string.default_address))
        setUserInfo(phoneNumber, userEmail, userAddress)
    }

    private fun setUserInfo(phoneNr: String?, userEmail: String?, userAddress: String?){
        val sharedPreferences = activity?.getSharedPreferences("UserInfoPreferences", Context.MODE_PRIVATE) ?: return
        with(sharedPreferences.edit()) {
            phoneNr?.let {
                phoneNrEditText.text = it
                this?.putString("phone_nr", it)
            }
            userEmail?.let {
                emailEditText.text = it
                this?.putString("email", it)
            }
            userAddress?.let {
                addressEditText.text = it
                this?.putString("address", it)
            }
            this.apply()
        }

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
            showPopupDialog()
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

    private fun showPopupDialog() {
        val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom).create()
        val view = requireActivity().layoutInflater.inflate(R.layout.user_info_edit, null)
        val confirmButton = view.findViewById(R.id.confirm_btn) as MaterialButton
        confirmButton.setOnClickListener {
            dialog.dismiss()
            showSnackBar("Uppgifter ändrade")
            changeUserProfile(view)
        }
        val cancelButton = view.findViewById(R.id.cancel_btn) as MaterialButton
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.apply {
            setTitle("Ändra Uppgifter")
            setView(view)
            show()
        }
    }

    private fun changeUserProfile(view: View) {
        setUserInfo(view.phoneNrEditText.text.toString(), view.emailEditText.text.toString(), view.addressEditText.text.toString())
    }



//TODO Lägg till en knapp för att rensa sök historiken
    companion object {
        val TAG : String = "ProfileFragment"
    }
}
