package com.restpod.podmonitor.test1

import java.util.*

/**
 * Created by Marc on 12/1/2017.
 */
data class ArduinoResponse(val isPumpRelayOn: Boolean,
                           val isHeaterRelayOn: Boolean,
                           val isUvLightRelayOn: Boolean,
                           val isDoser1RelayOn: Boolean,
                           val isDoser2RelayOn: Boolean,
                           val isWaterFlowOn: Boolean,
                           val isLightOn: Boolean,
                           val temperature: Double,
                           val lightColor: String)