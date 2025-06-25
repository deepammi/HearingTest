package com.hearingtest.hearingtest

import com.hearingtest.hearingtest.models.State

interface StateChangeListener
{
    fun onChanged(state: State, progressMax: Int, progressCurrent: Int, approxStop: Float, confirmation: Int)

    fun onFinish()
}