package org.rfcx.guardian.admin.device.android.wifi

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.device.android.system.DeviceSystemService
import org.rfcx.guardian.utility.rfcx.RfcxComm
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

object WifiCommunicationUtils {

    private val LOGTAG = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "WifiCommunicationUtils")

    private lateinit var serverSocket: ServerSocket
    private lateinit var socket: Socket
    private lateinit var streamInput: DataInputStream
    private lateinit var serverThread: Thread

    fun startServerSocket(context: Context) {
        serverThread = Thread(Runnable {
            try {
                val app = context.applicationContext as RfcxGuardian

                serverSocket = ServerSocket(9999)

                while (true) {
                    socket = serverSocket.accept()

                    streamInput = DataInputStream(socket.getInputStream())

                    val data = streamInput.readUTF()

                    if (!data.isNullOrBlank()) {
                        Log.d("ServerSocket", "Receiving data from Client: $data")

                        val command = data.split(":")[0]
                        val receiveJson = JSONObject(data)

                        //send response back
                        val streamOut = DataOutputStream(socket.getOutputStream())
                        when (receiveJson.get("command")) {
                            "prefs" -> {
                                try {
                                    val jsonArray = app.rfcxPrefs.prefsAsJsonArray
                                    val prefsJson = JSONObject().put("prefs", jsonArray)
                                    streamOut.writeUTF(prefsJson.toString())
                                    streamOut.flush()
                                } catch (e: JSONException) {
                                    Log.e(LOGTAG, e.toString())
                                }
                            }
                            "connection" -> {
                                streamOut.writeUTF(getConnectionResponse())
                                streamOut.flush()
                            }
                            "diagnostic" -> {
                            }
                            "configure" -> {
                                try {
                                    val jsonArray = RfcxComm.getQueryContentProvider("guardian", "configuration", "configuration", context.contentResolver)
                                    if (jsonArray.length() > 0) {
                                        val jsonObject = jsonArray.getJSONObject(0)
                                        val configurationJson = JSONObject().put("configure", jsonObject)
                                        streamOut.writeUTF(configurationJson.toString())
                                        streamOut.flush()
                                    }
                                } catch (e: JSONException) {
                                    Log.e(LOGTAG, e.toString())
                                }
                            }
                            "signal" -> {
                                val signal = DeviceSystemService.getSignalStrength().gsmSignalStrength // strength values (0-31, 99) as defined in TS 27.007 8.5,
                                val signalValue = (-113 + (2 * signal)) // converting signal strength to decibel-milliwatts (dBm)
                                val isSimCardInserted = app.deviceMobilePhone.hasSim()
                                try {
                                    val signalJson = JSONObject()
                                    signalJson.put("signal", signalValue)
                                    signalJson.put("sim_card", isSimCardInserted)
                                    streamOut.writeUTF(signalJson.toString())
                                    streamOut.flush()
                                } catch (e: JSONException) {
                                    Log.e(LOGTAG, e.toString())
                                }
                            }
                            else -> {
                                val commandObject = JSONObject(receiveJson.get("command").toString())
                                val commandKey = commandObject.keys().asSequence().toList()[0]
                                when(commandKey){
                                    "sync" -> {
                                        val jsonArray = commandObject.getJSONArray("sync")
                                        var prefResponse = JSONArray()
                                        var syncResponse = ""
                                        try {
                                            for (i in 0 until jsonArray.length()) {
                                                val pref = jsonArray.get(i)
                                                Log.d(LOGTAG, pref.toString())
                                                prefResponse = RfcxComm.getQueryContentProvider("guardian", "prefs_set", pref.toString(), context.contentResolver)
                                            }
                                            if (prefResponse.length() > 0) {
                                                syncResponse = getSyncResponse("success")
                                            }
                                        } catch (e: JSONException) {
                                            Log.e(LOGTAG, e.toString())
                                            syncResponse = getSyncResponse("failed")
                                        } finally {
                                            streamOut.writeUTF(syncResponse)
                                            streamOut.flush()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    streamInput.close()
                }
            } catch (e: Exception) {
                Log.e(LOGTAG, e.toString())
            }
        })

        serverThread.start()
    }

    fun stopServerSocket() {
        serverThread.interrupt()
        socket.close()
        streamInput.close()
        serverSocket.close()
    }

    private fun getConnectionResponse(): String {
        val response = JSONObject()
        val status = JSONObject()
        status.put("status", "success")
        response.put("connection", status)

        return response.toString()
    }

    private fun getSyncResponse(result: String): String {
        val response = JSONObject()
        val status = JSONObject()
        status.put("status", result)
        response.put("sync", status)

        return response.toString()
    }
}
