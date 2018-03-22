package com.restpod.podmonitor.test1

import com.restpod.podmonitor.PodStatus
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Created by Marc on 12/1/2017.
 */

class ArduinoManager(_arduinoResponse: ArduinoResponse, _podStatus: PodStatus)
{
    var lastResponse = _arduinoResponse
    var newResponse = _arduinoResponse
    var podStatus = _podStatus
    var pumpChanged = lastResponse.isPumpRelayOn == newResponse.isPumpRelayOn
    var heaterChanged = lastResponse.isHeaterRelayOn == newResponse.isHeaterRelayOn
    var temperatureChanged = lastResponse.temperature == newResponse.temperature
    var waterFlowCounter = 0
    var setupSession = false

    fun getArduinoCommand(_newResponse: ArduinoResponse): ArduinoCommand
    {
        lastResponse = newResponse
        newResponse = _newResponse

        pumpChanged = lastResponse.isPumpRelayOn != newResponse.isPumpRelayOn
        heaterChanged = lastResponse.isHeaterRelayOn != newResponse.isHeaterRelayOn
        temperatureChanged = lastResponse.temperature != newResponse.temperature

        return getArduinoCommand()
    }
    fun getArduinoCommand(_podStatus: PodStatus): ArduinoCommand
    {
        setupSession = _podStatus.sessionEnd != null && podStatus.sessionEnd != _podStatus.sessionEnd

        podStatus = _podStatus
        return getArduinoCommand()
    }
    private fun getArduinoCommand(): ArduinoCommand
    {
        val currentDate = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
        var commandToIssue = ArduinoCommand.None

        //ALERT: IF WATER SENSOR IS FAILING
        if (!newResponse.isWaterFlowOn && (newResponse.isPumpRelayOn || newResponse.isHeaterRelayOn))
        {
            waterFlowCounter++
            if (waterFlowCounter > 3) {
                return ArduinoCommand.TurnOff
                // THROW ALERT
            }
        }
        else if (waterFlowCounter > 0)
            waterFlowCounter = 0

        if (newResponse.isWaterFlowOn && //Only turn heater on after flow has been confirmed
                (newResponse.temperature < podStatus.targetTemperature!! - 1 ||
                    (newResponse.isPumpRelayOn && newResponse.temperature < podStatus.targetTemperature!! - .3)))
            commandToIssue = ArduinoCommand.TurnHeaterOn
        else if (!newResponse.isWaterFlowOn && // Heater needs to come on, but pump turns on first
                (newResponse.temperature < podStatus.targetTemperature!! - 1 ||
                        (newResponse.isPumpRelayOn && newResponse.temperature < podStatus.targetTemperature!! - .3)))
            commandToIssue = ArduinoCommand.TurnPumpOn
        else if (podStatus.cleaningEnd != null && podStatus.cleaningEnd!! > currentDate)
            commandToIssue = ArduinoCommand.TurnPumpOn
        else
            commandToIssue = ArduinoCommand.TurnOff
        if (podStatus.sessionEnd != null && podStatus.sessionEnd!! > currentDate)
            commandToIssue = ArduinoCommand.TurnOff

        if ((commandToIssue == ArduinoCommand.TurnHeaterOn && newResponse.isPumpRelayOn && newResponse.isHeaterRelayOn) ||
                (commandToIssue == ArduinoCommand.TurnPumpOn && newResponse.isPumpRelayOn && !newResponse.isHeaterRelayOn) ||
                (commandToIssue == ArduinoCommand.TurnOff && !newResponse.isPumpRelayOn && !newResponse.isHeaterRelayOn))
            commandToIssue = ArduinoCommand.None

        if (podStatus.targetTemperature!! < 32 || podStatus.targetTemperature!! > 100)
            commandToIssue = ArduinoCommand.TurnOff

        if (commandToIssue != ArduinoCommand.None)
            waterFlowCounter++
        return commandToIssue
    }
}
enum class ArduinoCommand {
    None, TurnPumpOn, TurnHeaterOn, TurnOff,
    TurnDoser1On, TurnDoser1Off, TurnDoser2On, TurnDoser2Off,
    TurnLightRed, TurnLightGreen, TurnLightBlue, TurnLightPurple, TurnLightAqua, TurnLightOff
}