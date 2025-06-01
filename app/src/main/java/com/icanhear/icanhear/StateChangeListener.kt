package com.icanhear.icanhear

import com.icanhear.icanhear.models.State

interface StateChangeListener
{
    fun onChanged(state: State, progressMax: Int, progressCurrent: Int, approxStop: Float, confirmation: Int)

    fun onFinish()
}