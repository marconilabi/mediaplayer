package com.restpod.podmonitor

import java.util.*

/**
 * Created by Marc on 10/9/2017.
 */
data class PodStatus(val podGuid: String,
                     val cleaningEnd: Date?,
                     val sessionEnd: Date?,
                     val targetTemperature: Double?,
                     val musicList: List<PodStatusMusicTrack>?)


data class PodStatusMusicTrack(val url: String,
                     val startTime: Date,
                     val endTime: Date,
                     val trackType: Int,
                     val volume: Double,
                     val id: Int)