package com.restpod.podmonitor.test1

import java.util.*

/**
 * Created by Marc on 10/9/2017.
 */
data class PodSchedule(val actionTypeId: String,
                       val minutes: Int,
                       val repeatInterval: String,
                       val StartDate: Date)