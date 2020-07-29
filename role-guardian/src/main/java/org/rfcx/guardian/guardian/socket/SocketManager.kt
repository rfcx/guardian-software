package org.rfcx.guardian.guardian.socket

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxComm
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

object SocketManager {

    private val LOGTAG = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SocketManager")

    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private var socket: Socket? = null

    private var inComingMessageThread: Thread? = null

    private var streamInput: DataInputStream? = null

    private var streamOutput: DataOutputStream? = null

    private var context: Context? = null
    private var app: RfcxGuardian? = null

    private var isMicTesting: Boolean = false

    fun startServerSocket(context: Context) {
        this.context = context
        app = context.applicationContext as RfcxGuardian
        serverThread = Thread(Runnable {
            try {
                serverSocket = ServerSocket()
                serverSocket?.reuseAddress = true
                serverSocket?.bind(InetSocketAddress(9999))

                startInComingMessageThread()
                while (true) {
                    socket = serverSocket?.accept()
                }
            } catch (e: Exception) {
                RfcxLog.logExc(LOGTAG, e)
            }
        })
        serverThread?.start()
    }

    private fun startInComingMessageThread() {
        inComingMessageThread = Thread(Runnable {
            try {
                while (true) {
                    streamInput = DataInputStream(socket?.getInputStream())
                    val message = streamInput?.readUTF()

                    if (!message.isNullOrBlank()) {
                        Log.d("ServerSocket", "Receiving data from Client: $message")

                        val receiveJson = JSONObject(message)

                        //send response back
                        streamOutput = DataOutputStream(socket?.getOutputStream())
                        when (receiveJson.get("command")) {
                            "prefs" -> sendPrefsMessage()
                            "connection" -> sendConnectionMessage()
                            "diagnostic" -> sendDiagnosticMessage()
                            "configure" -> sendConfigurationMessage()
                            "microphone_test" -> sendMicrophoneTestMessage()
                            "signal" -> sendSignalMessage()
                            else -> {
                                val commandObject =
                                    JSONObject(receiveJson.get("command").toString())
                                val commandKey = commandObject.keys().asSequence().toList()[0]
                                when (commandKey) {
                                    "sync" -> sendSyncConfigurationMessage(
                                        commandObject.getJSONArray(
                                            "sync"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                RfcxLog.logExc(LOGTAG, e)
            }
        })
        inComingMessageThread?.start()
    }

    fun stopServerSocket() {
        //stop server thread
        serverThread?.interrupt()
        serverThread = null

        //stop incoming message thread
        inComingMessageThread?.interrupt()
        inComingMessageThread = null

        socket?.close()
        streamInput?.close()
        streamOutput?.close()
        serverSocket?.close()
    }

    private fun sendPrefsMessage() {
        try {
            val prefsJsonArray = app?.rfcxPrefs?.prefsAsJsonArray
            val prefsJson = JSONObject().put("prefs", prefsJsonArray)
            streamOutput?.writeUTF(prefsJson.toString())
            streamOutput?.flush()
        } catch (e: JSONException) {
            Log.e(LOGTAG, e.toString())
        }
    }

    private fun sendConnectionMessage() {
        try {
            streamOutput?.writeUTF(getConnectionResponse())
            streamOutput?.flush()
        } catch (e: JSONException) {
            Log.e(LOGTAG, e.toString())
        }
    }

    private fun sendDiagnosticMessage() {
        try {
            val diagnosticJson = JSONObject()
            val diagnosticJsonArray = RfcxComm.getQueryContentProvider(
                "guardian",
                "diagnostic",
                "diagnostic",
                context?.contentResolver
            )
            if (diagnosticJsonArray.length() > 0) {
                val jsonObject = diagnosticJsonArray.getJSONObject(0)
                diagnosticJson.put("diagnostic", jsonObject)
            }

            val configureJsonArray = RfcxComm.getQueryContentProvider(
                "guardian",
                "configuration",
                "configuration",
                context?.contentResolver
            )
            if (configureJsonArray.length() > 0) {
                val jsonObject = configureJsonArray.getJSONObject(0)
                diagnosticJson.put("configure", jsonObject)
            }

            val prefsJsonArray = app?.rfcxPrefs?.prefsAsJsonArray
            diagnosticJson.put("prefs", prefsJsonArray)

            streamOutput?.writeUTF(diagnosticJson.toString())
            streamOutput?.flush()
        } catch (e: JSONException) {
            Log.e(LOGTAG, e.toString())
        }
    }

    private fun sendConfigurationMessage() {
        try {
            val jsonArray = RfcxComm.getQueryContentProvider(
                "guardian",
                "configuration",
                "configuration",
                context?.contentResolver
            )
            if (jsonArray.length() > 0) {
                val jsonObject = jsonArray.getJSONObject(0)
                val configurationJson =
                    JSONObject().put("configure", jsonObject)
                streamOutput?.writeUTF(configurationJson.toString())
                streamOutput?.flush()
            }
        } catch (e: JSONException) {
            Log.e(LOGTAG, e.toString())
        }
    }

    private fun sendMicrophoneTestMessage() {
        isMicTesting = !isMicTesting
        var tempByte = ""
        try {
            while (isMicTesting) {
                val audioPair = app?.audioCaptureUtils?.audioBuffer
                val audioString = Base64.encodeToString(audioPair!!.first, Base64.NO_WRAP)
                val audioReadSize = audioPair.second
                if (tempByte != audioString) {
                    tempByte = audioString
                    val audioJsonObject = JSONObject()
                        .put("buffer", tempByte)
                        .put("read_size", audioReadSize)
                    val jsonObject = JSONObject().put(
                        "microphone_test",
                        audioJsonObject
                    )
                    streamOutput?.writeUTF(jsonObject.toString())
                }
            }
        } catch (e: JSONException) {
            Log.e(LOGTAG, e.toString())
        }
    }

    private fun sendSignalMessage() {
        val signalJsonArray =
            RfcxComm.getQueryContentProvider("admin", "signal", "signal", context?.contentResolver)
        if (signalJsonArray.length() > 0) {
            val signalStrength = signalJsonArray.getJSONObject(0).getInt("signal")
            val signalValue =
                (-113 + (2 * signalStrength)) // converting signal strength to decibel-milliwatts (dBm)
            val isSimCardInserted = app?.deviceMobilePhone?.hasSim()
            try {
                val signalJson = JSONObject()
                    .put("signal", signalValue)
                    .put("sim_card", isSimCardInserted)
                val signalInfoJson = JSONObject()
                    .put("signal_info", signalJson)
                streamOutput?.writeUTF(signalInfoJson.toString())
                streamOutput?.flush()
            } catch (e: JSONException) {
                Log.e(LOGTAG, e.toString())
            }
        }
    }

    private fun sendSyncConfigurationMessage(syncJSONArray: JSONArray) {
        var prefResponse = JSONArray()
        var syncResponse = ""
        try {
            for (i in 0 until syncJSONArray.length()) {
                val pref = syncJSONArray.get(i)
                Log.d(LOGTAG, pref.toString())
                prefResponse = RfcxComm.getQueryContentProvider(
                    "guardian",
                    "prefs_set",
                    pref.toString(),
                    context?.contentResolver
                )
            }
            if (prefResponse.length() > 0) {
                syncResponse = getSyncResponse("success")
            }
        } catch (e: JSONException) {
            Log.e(LOGTAG, e.toString())
            syncResponse = getSyncResponse("failed")
        } finally {
            streamOutput?.writeUTF(syncResponse)
            streamOutput?.flush()
        }
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
