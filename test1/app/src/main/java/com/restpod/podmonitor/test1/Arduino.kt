package com.restpod.podmonitor

import android.util.Log
import com.fasterxml.jackson.module.kotlin.*
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDeviceCallback
import com.google.android.things.pio.UartDevice
import com.restpod.podmonitor.test1.ArduinoResponse
import java.io.IOException

class Arduino(_mainActivity: MainActivity): AutoCloseable {
    var mainActivity = _mainActivity
    private val uartDeviceCallBack = CustomUartCallBack()
    private val TAG = "Arduino"
    private val deviceName = "UART6"
    private val uart: UartDevice by lazy {
        PeripheralManager.getInstance().openUartDevice(deviceName).apply {
            setBaudrate(9600)
            setDataSize(8)
            setParity(UartDevice.PARITY_NONE)
            setStopBits(1)
            registerUartDeviceCallback(uartDeviceCallBack)
        }
    }

/*    fun read(): String {
        val maxCount = 8
        val buffer = ByteArray(maxCount)
        var output = ""
        do {
            val count = uart.read(buffer, buffer.size)
            output += buffer.toReadableString()
            if(count == 0) break
            Log.d(TAG, "Read ${buffer.toReadableString()} $count bytes from peripheral")
        } while (true)
        return output
    }*/

    private fun ByteArray.toReadableString() = filter { it > 0.toByte() }
            .joinToString(separator = "") { it.toChar().toString() }

    fun write(value: String) {
        val count = uart.write(value.toByteArray(), value.length)
        Log.d(TAG, "Wrote $value $count bytes to peripheral")
    }

    override fun close() {
        uart.unregisterUartDeviceCallback(uartDeviceCallBack)
        uart.close()
    }

    inner class CustomUartCallBack : UartDeviceCallback{

        override fun onUartDeviceDataAvailable(uart: UartDevice): Boolean {
            try {
                val maxCount = 8
                val buffer = ByteArray(maxCount)
                var output = ""
                do {
                    val count = uart.read(buffer, buffer.size)
                    output += buffer.toReadableString()
                    if(count == 0) break
                } while (true)

                if (output.contains("button", true))
                {
                    val buttonIndex = output.indexOf("Button")
                    val buttonNumber = output.substring(buttonIndex + 6, buttonIndex + 7).toInt()
                    mainActivity.handleButtonPress(buttonNumber)
                }
                else
                {
                    val mapper = jacksonObjectMapper()
                    try {
                        val arduinoResponse = mapper.readValue<ArduinoResponse>(output)
                        mainActivity.handleArduinoResponse(arduinoResponse)
                    }
                    catch (e: Exception){
                        mainActivity.txtArduino!!.text = e.message
                    }
                }
            } catch (e : IOException){
                Log.e("Tag", "error while reading uart buffer",e)
            }
            return true
        }

        override fun onUartDeviceError(uart: UartDevice, error: Int) {
            Log.e("Tag", "error $error in CustomUartCallBack")
        }

    }
}