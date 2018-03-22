package com.restpod.podmonitor.test1

import java.util.*
import java.time.*

/**
 * Created by Marc on 10/9/2017.
 */
data class PodBusinessHours(val OpenMonday: Duration,
                       val CloseMonday: Duration,
                       val OpenTuesday: Duration,
                       val CloseTuesday: Duration,
                       val OpenWednesday: Duration,
                       val CloseWednesday: Duration,
                       val OpenThursday: Duration,
                       val CloseThursday: Duration,
                       val OpenFriday: Duration,
                       val CloseFriday: Duration,
                       val OpenSaturday: Duration,
                       val CloseSaturday: Duration,
                       val OpenSunday: Duration,
                       val CloseSunday: Duration)