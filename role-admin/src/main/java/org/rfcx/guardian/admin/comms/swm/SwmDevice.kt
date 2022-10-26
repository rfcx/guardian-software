package org.rfcx.guardian.admin.comms.swm

import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.control.SwmPower
import org.rfcx.guardian.admin.comms.swm.data.SwmDTResponse

class SwmDevice(private val api: SwmApi, private val power: SwmPower) {

    private var id: String? = null
    private var isGPSConnected: Boolean? = null
    private var isSleeping = false

    fun getId(): String? {
        if (id == null) {
            id = api.getSwarmDeviceId()
        }
        awake()
        return id
    }

    fun getGPSConnection(): Boolean? {
        if (isGPSConnected == null) {
            isGPSConnected = api.getGPSConnection() != null
        }
        awake()
        return isGPSConnected
    }

    fun getSystemTime(): SwmDTResponse? {
        val time = api.getDateTime()
        awake()
        return time
    }

    fun powerOn() {
        power.on = true
    }

    fun powerOff() {
        power.on = false
    }

    fun isSleeping() = isSleeping

    fun sleep() {
        api.sleep()
        isSleeping = true
    }

    fun awake() {
        isSleeping = false
    }
}
