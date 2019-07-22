package org.rfcx.guardian.guardian.receiver

import android.annotation.SuppressLint
import android.app.IntentService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxLog



class BluetoothReceiver : BroadcastReceiver(){

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BluetoothReceiver::class.java)

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val device = intent!!.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0)
                //the pin in case you need to accept for an specific pin
                Log.d(logTag, " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0))
                //maybe you look for a name or address
                Log.d(logTag, device.name)
                val pinBytes: ByteArray
                pinBytes = ("" + pin).toByteArray(charset("UTF-8"))
                device.setPin(pinBytes)
                //setPairing confirmation if neeeded
                device.setPairingConfirmation(true)
                context!!.startService(Intent(context, BTTethering::class.java))



            } catch (e: Exception) {
                e.printStackTrace()
            }
    }
}
class BTTethering: IntentService("EnableTethering"){
    override fun onHandleIntent(intent: Intent?) {
//        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val btPan = Class.forName("android.bluetooth.BluetoothPan")
        val btPanCtor = btPan.getDeclaredConstructor(Context::class.java, BluetoothProfile.ServiceListener::class.java)
        btPanCtor.isAccessible = true
        val instance = btPanCtor.newInstance(applicationContext, BTPanServiceListener(applicationContext))
    }

}

class BTPanServiceListener(private val context: Context?) : BluetoothProfile.ServiceListener {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BTPanServiceListener::class.java)

    override fun onServiceConnected(
        profile: Int,
        proxy: BluetoothProfile
    ) {
        //Some code must be here or the compiler will optimize away this callback.
        Log.i(logTag, "BTPan proxy connected")
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("AA:BB:CC:DD:EE:FF")
        try {
            val connectMethod = proxy.javaClass.getDeclaredMethod("connect", BluetoothDevice::class.java)
            if (!(connectMethod.invoke(proxy, device) as Boolean)) {
                Log.e(logTag, "Unable to start connection")
            }
        } catch (e: Exception) {
            Log.e(logTag, "Unable to reflect android.bluetooth.BluetoothPan", e)
        }
        val paramSet = arrayOfNulls<Class<*>>(1)
        paramSet[0] = Boolean::class.javaPrimitiveType
        val param = Boolean::class.javaPrimitiveType
        val setTetheringOn = proxy.javaClass.getDeclaredMethod("setBluetoothTethering", param!!)
        setTetheringOn.invoke(proxy, true)

    }

    override fun onServiceDisconnected(profile: Int) {
        Log.i(logTag, "BTPan proxy not connected")
    }
}