package org.rfcx.guardian.admin.device.i2c.sentry.infineon

import android.content.Context
import org.rfcx.guardian.admin.RfcxGuardian
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class Infineon(context: Context) {

    companion object {
        const val MAIN_ADDRESS_REG = "0x28"
        const val STATUS_REG = "0x01"
        const val PRESSURE_H_REG = "0x0b"
        const val PRESSURE_L_REG = "0x0c"
        const val MODE_REG = "0x04"
        const val CO2_H_REG = "0x05"
        const val CO2_L_REG = "0x06"
    }

    private enum class Mode(val value: Byte)  {
        IDLE(0b00), SINGLE(0b01)
    }

    private val app = context.applicationContext as RfcxGuardian
    private var pressure = 1013L

    init {
        if (readRegValue(STATUS_REG) > 0) {
            pressure = getPressure()
            setPressure()
            setMode(Mode.IDLE)
        }
    }

    fun getCO2Value(): Int {
        setMode(Mode.SINGLE)

        val co2High = readRegValue(CO2_H_REG)
        val co2Low = readRegValue(CO2_L_REG)

        return combineHighAndLowRegValues(co2High, co2Low)
    }

    private fun setPressure(pressure: Long = 1013L /* default hPa */) {
        if (this.pressure == pressure) return

        if (getMode() != Mode.IDLE) {
            setMode(Mode.IDLE)
        }

        val pressureArr = BigInteger.valueOf(pressure).toByteArray()
        if (pressureArr.size == 2) {
            writeRegValue(PRESSURE_H_REG, pressureArr[0])
            writeRegValue(PRESSURE_L_REG, pressureArr[1])
            sleep(400)
        } else {
            /* above than 4 bytes */
        }
    }

    private fun getPressure(): Long {
        val presHigh = readRegValue(PRESSURE_H_REG)
        val presLow = readRegValue(PRESSURE_L_REG)
        return combineHighAndLowRegValues(presHigh, presLow).toLong()
    }

    private fun setMode(mode: Mode) {
        if (getMode() == mode) return
        writeRegValue(MODE_REG, mode.value)
    }

    private fun getMode(): Mode {
        return when (readRegValue(MODE_REG)) {
            Mode.IDLE.value -> Mode.IDLE
            Mode.SINGLE.value -> Mode.SINGLE
            else -> Mode.IDLE
        }
    }

    private fun readRegValue(reg: String): Byte {
        val value = app.deviceI2cUtils.i2cGetAsByte(reg, MAIN_ADDRESS_REG, true, false)
        sleep(5)
        return value
    }

    private fun writeRegValue(reg: String, byte: Byte) {
        app.deviceI2cUtils.i2cSet(reg, MAIN_ADDRESS_REG, byte.toInt(), false)
        sleep(400)
    }

    private fun combineHighAndLowRegValues(high: Byte, low: Byte): Int {
        return (high.toInt() and 0xFF) shl 8 or (low.toInt() and 0xFF)
    }

    private fun sleep(milli: Long) {
        TimeUnit.MILLISECONDS.sleep(milli)
    }

}