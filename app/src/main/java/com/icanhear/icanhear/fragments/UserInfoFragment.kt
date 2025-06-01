package com.icanhear.icanhear.fragments

import android.os.Bundle
import android.text.BoringLayout
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.android.billingclient.api.*
import com.google.android.material.snackbar.Snackbar
import com.icanhear.icanhear.R
import com.icanhear.icanhear.models.UserInfo
import com.icanhear.icanhear.databinding.FragmentUserInfoBinding
import com.icanhear.icanhear.inapp.InAppBilling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//import kotlinx.android.synthetic.main.fragment_user_info.view.*

class UserInfoFragment : Fragment() {

    private lateinit var binding: FragmentUserInfoBinding
    private lateinit var billingClient: BillingClient
    private var inAppBilling: InAppBilling? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserInfoBinding.inflate(inflater, container, false)

        with(binding) {
            name.setTitle("Name")
            location.setTitle("Location")
            age.setTitle("Age")
            email.setTitle("Email")
            phone.setTitle("Phone")
            history.setTitle("Medical History")
            gainFactor.setTitle("Gain Factor 1 to between 100")
            next.setOnClickListener(this@UserInfoFragment::next)
            supportUs.setOnClickListener(this@UserInfoFragment::billingLibrary)
        }

        return binding.root
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        initInputViews()
    }

    private fun initInputViews() {
        val userInfo = UserInfo.instance
        with(binding) {
            name.contentEditText?.setText(userInfo.name)
            location.contentEditText?.setText(userInfo.location)
            age.contentEditText?.setText(userInfo.age)
            email.contentEditText?.setText(userInfo.email)
            phone.contentEditText?.setText(userInfo.phone)
            history.contentEditText?.setText(userInfo.medicalHistory)
            gainFactor.contentEditText?.setText(userInfo.gainFactor)

            age.contentEditText?.inputType = InputType.TYPE_CLASS_NUMBER
            email.contentEditText?.inputType = InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            phone.contentEditText?.inputType = InputType.TYPE_CLASS_PHONE
            gainFactor.contentEditText?.inputType = InputType.TYPE_CLASS_NUMBER
//            setupBillingClient()
            inAppBilling = InAppBilling()
            inAppBilling!!.setupBillingClient(requireContext())

        }
    }

    fun next(view: View) {
        with(binding) {
            if (TextUtils.isEmpty(name.contentEditText?.text)) {
                showMessage(view, "Name is mandatory")
                return
            }
            if (TextUtils.isEmpty(age.contentEditText?.text)) {
                showMessage(view, "Age is mandatory")
                return
            }

            val userInfo = UserInfo.instance
            userInfo.name = name.contentEditText?.text.toString()
            userInfo.location = location.contentEditText?.text.toString()
            userInfo.age = age.contentEditText?.text.toString()
            userInfo.email = email.contentEditText?.text.toString()
            userInfo.phone = phone.contentEditText?.text.toString()
            userInfo.medicalHistory = history.contentEditText?.text.toString()

//            if (gainFactor.contentEditText?.visibility == View.VISIBLE && TextUtils.isEmpty(
//                    gainFactor.contentEditText?.text
//                )
//            ) {
//                showMessage(view, "Gain Factor is mandatory")
//                return
//            } else
            if (gainFactor.contentEditText?.visibility == View.VISIBLE && !TextUtils.isEmpty(
                    gainFactor.contentEditText?.text
                )
            ) {
                userInfo.gainFactor = gainFactor.contentEditText?.text.toString()
            } else {
                userInfo.gainFactor = ""
            }



            userInfo.save()

            view.findNavController().navigate(R.id.hearingTestFragment)
        }
    }


    private fun billingLibrary(view: View) {
        inAppBilling!!.loadBilling(requireActivity())

    }

    private fun showMessage(view: View, message: String) {
        context?.let {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }


}

