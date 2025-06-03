package com.icanhear.hearingtest

import com.icanhear.hearingtest.models.State

interface StateChangeListener
{
    fun onChanged(state: State, progressMax: Int, progressCurrent: Int, approxStop: Float, confirmation: Int)

    fun onFinish()
}