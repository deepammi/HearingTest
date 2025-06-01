package com.icanhear.icanhear.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.icanhear.icanhear.R
import com.icanhear.icanhear.databinding.FragmentWelcomeBinding

class WelcomeFragment: Fragment() {

    private lateinit var binding: FragmentWelcomeBinding

    private val intentFilter = IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
    private val myNoisyAudioStreamReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                if (intent.hasExtra("state")){
                    val pluggedIn = intent.getIntExtra("state", 0) == 1
                    binding.start.isEnabled = pluggedIn
                    binding.headphoneNotification.visibility = if (pluggedIn) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)

        with(binding) {
            start.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.userInfoFragment))
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.registerReceiver(myNoisyAudioStreamReceiver, intentFilter)
    }

    override fun onDestroyView() {
        try {
            activity?.unregisterReceiver(myNoisyAudioStreamReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroyView()
    }
}