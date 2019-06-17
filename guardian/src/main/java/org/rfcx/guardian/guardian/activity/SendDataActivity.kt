package org.rfcx.guardian.guardian.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.*
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_syncguardian.*
import org.rfcx.guardian.guardian.RfcxGuardian
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import android.content.Intent
import android.content.BroadcastReceiver
import org.rfcx.guardian.guardian.R


var devices = ArrayList<BluetoothDevice>()
var devicesMap = HashMap<String, BluetoothDevice>()
var mArrayAdapter: ArrayList<String>? = null
val uuid: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
//val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
var message = ""
var app: RfcxGuardian? = null

class SendDataActivity : AppCompatActivity() {

    private val activity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_syncguardian)

        app = application as RfcxGuardian

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)

        val filter2 = IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST")
        registerReceiver(paringRequest, filter2)

        val filter3 = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(mReceiver2, filter3)


        devicesMap = HashMap()
        devices = ArrayList()

        message = guardian_edittext.text.toString()
        guardian_edittext.text.clear()
        getPairedDevices()

        BluetoothServerController(this).start()
    }

    private fun getPairedDevices() {
        object : Thread() {
            override fun run() {
                while (true) {
                    try {
                        runOnUiThread {
                            for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
                                devicesMap = HashMap()
                                devices = ArrayList()
                                devicesMap.put(device.address, device)
                                devices.add(device)
                                if (mArrayAdapter == null) {
                                    mArrayAdapter?.add((if (device.name != null) device.name else "Unknown") + "\n" + device.address + "\nPared")
                                }else{
                                    if(mArrayAdapter!!.contains(device.name)){
                                        mArrayAdapter?.add((if (device.name != null) device.name else "Unknown") + "\n" + device.address + "\nPared")
                                    }
                                }
                                // Add the name and address to an array adapter to show in a ListView
                            }
                        }
                        sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                }
            }
        }.start()
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val pairedDevice = devicesMap[device.address]
                if (pairedDevice == null) {
                    var index = -1
                    for (i in devices.indices) {
                        val tmp = devices[i]
                        if (tmp.address == device.address) {
                            index = i
                            break
                        }
                    }

                    if (index > -1) {
                        if (device.name != null) {
                            mArrayAdapter?.add(
                                (if (device.name != null) device.name else "Unknown")
                            )
                        }
                    } else {
                        devices.add(device)
                        // 	Add the name and address to an array adapter to show in a ListView
                        mArrayAdapter?.add((if (device.name != null) device.name else "Unknown") + "\n" + device.address)
                    }
                }
            }
        }
    }

    private val paringRequest = object: BroadcastReceiver(){
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.bluetooth.device.action.PAIRING_REQUEST") {
                try {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0)
                    //the pin in case you need to accept for an specific pin
                    Log.d("PIN", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0))
                    //maybe you look for a name or address
                    Log.d("Bonded", device.name)
                    val pinBytes: ByteArray
                    pinBytes = ("" + pin).toByteArray(charset("UTF-8"))
                    device.setPin(pinBytes)
                    //setPairing confirmation if neeeded
                    device.setPairingConfirmation(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val mReceiver2 = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED){
                val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0)
                when(btState){
                    BluetoothAdapter.STATE_CONNECTED -> {
                       // BluetoothServerController(activity).start()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        unregisterReceiver(paringRequest)
        unregisterReceiver(mReceiver2)
    }

}


class BluetoothServerController(activity: SendDataActivity) : Thread() {
    private var cancelled: Boolean
    private val serverSocket: BluetoothServerSocket?
    private val activity = activity

    init {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("test", uuid)
            this.cancelled = false
        } else {
            this.serverSocket = null
            this.cancelled = true
        }

    }

    override fun run() {
        var socket: BluetoothSocket

        while (true) {
            if (this.cancelled) {
                break
            }

            try {
                socket = serverSocket!!.accept()
            } catch (e: IOException) {
                break
            }

            if (!this.cancelled && socket != null) {
                Log.i("server", "Connecting")
                BluetoothServer(this.activity, socket).start()
            }
        }
    }

    fun cancel() {
        this.cancelled = true
        this.serverSocket!!.close()
    }
}

class BluetoothServer(private val activity: SendDataActivity, private val socket: BluetoothSocket) : Thread() {
    private val inputStream = this.socket.inputStream
    private val outputStream = this.socket.outputStream

    override fun run() {
        try {
            val buffer = ByteArray(1024)
            Log.i("server", "Reading")
            val bytes = inputStream.read(buffer)
            val text = String(buffer, 0, bytes)
            Log.i("server", "Message received")
            Log.i("server", text)

            var pos = 0
            if (text == "guid") {
                devices?.forEach { it ->
                    Log.i("server", "start sending guid ${it.name}")
                    BluetoothClient(it).start()
                }
            }

        } catch (e: Exception) {
            Log.e("client", "Cannot read data", e)
        } finally {
        }
    }
}

class BluetoothClient(device: BluetoothDevice) : Thread() {
    private val socket = device.createRfcommSocketToServiceRecord(uuid)

    override fun run() {
        Log.i("client", "Connecting")
        this.socket.connect()

        Log.i("client", "Sending")
        val outputStream = this.socket.outputStream
        val inputStream = this.socket.inputStream

        val guid = app?.rfcxDeviceGuid?.deviceGuid
        try {
            outputStream.write(guid?.toByteArray())
            outputStream.flush()
            Log.i("client", "Sent $guid")
        } catch (e: Exception) {
            Log.e("client", "Cannot send", e)
        } finally {
            outputStream.close()
            inputStream.close()
            socket.close()
        }
    }
}