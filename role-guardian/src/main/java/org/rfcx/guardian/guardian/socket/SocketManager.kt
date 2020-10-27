package org.rfcx.guardian.guardian.socket

import android.content.Context
import android.os.Looper
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.api.http.RegisterApi
import org.rfcx.guardian.guardian.api.http.SocketRegisterCallback
import org.rfcx.guardian.guardian.entity.RegisterRequest
import org.rfcx.guardian.utility.rfcx.RfcxComm
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

object SocketManager {

    private val LOGTAG = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SocketManager")

    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private var socket: Socket? = null

    private var streamInput: DataInputStream? = null

    private var streamOutput: DataOutputStream? = null

    private var context: Context? = null
    private var app: RfcxGuardian? = null

    private var requireCheckInTest = false

    fun startServerSocket(context: Context) {
        this.context = context
        app = context.applicationContext as RfcxGuardian
        serverThread = Thread(Runnable {
            Looper.prepare()
            try {
                serverSocket = ServerSocket(9999)
                serverSocket?.reuseAddress = true

                while (true) {
                    socket = serverSocket?.accept()
                    socket?.tcpNoDelay = true

                    streamInput = DataInputStream(socket?.getInputStream())
                    val message = streamInput?.readUTF()

                    if (!message.isNullOrBlank()) {

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
                            "checkin" -> {
                                requireCheckInTest = if (!requireCheckInTest) {
                                    sendCheckInTestMessage(CheckInState.NOT_PUBLISHED)
                                    true
                                } else {
                                    false
                                }
                            }
                            "sentinel" -> sendSentinelValues()
                            "is_registered" -> sendIfGuardianRegistered()
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
                                    "register" -> {
                                        val registerInfo = commandObject.getJSONObject("register")
                                        val tokenId = registerInfo.getString("token_id")
                                        val isProduction = registerInfo.getBoolean("is_production")
                                        sendRegistrationStatus(tokenId, isProduction)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                RfcxLog.logExc(LOGTAG, e)
            }
            Looper.loop()
        })
        serverThread?.start()
    }

    fun stopServerSocket() {
        //stop server thread
        serverThread?.interrupt()
        serverThread = null

        streamInput?.close()
        streamInput = null

        streamOutput?.close()
        streamOutput = null

        socket?.close()
        socket = null

        serverSocket?.close()
        serverSocket = null
    }

    private fun sendPrefsMessage() {
        try {
            val prefsJsonArray = app?.rfcxPrefs?.prefsAsJsonArray
            val prefsJson = JSONObject().put("prefs", prefsJsonArray)
            streamOutput?.writeUTF(prefsJson.toString())
            streamOutput?.flush()
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "null")
        }
    }

    private fun sendConnectionMessage() {
        try {
            streamOutput?.writeUTF(getConnectionResponse())
            streamOutput?.flush()
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "null")
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
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "null")
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
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "null")
        }
    }

    private fun sendMicrophoneTestMessage() {
        try {
            val audioPair = app?.audioCaptureUtils?.audioBuffer
            if (audioPair != null) {
                val audioString = Base64.encodeToString(audioPair.first, Base64.DEFAULT)
                val audioReadSize = audioPair.second
                val audioJsonObject = JSONObject()
                    .put("amount", 1)
                    .put("number", 1)
                    .put("buffer", audioString)
                    .put("read_size", audioReadSize)
                val micTestObject = JSONObject().put("microphone_test", audioJsonObject)
                if (micTestObject.toString().length <= 65535) {
                    streamOutput?.writeUTF(micTestObject.toString())
                    streamOutput?.flush()
                } else {
                    val audioChunks = audioPair.first.toSmallChunk(10)
                    audioChunks.forEachIndexed { index, audio ->
                        val readSize = audio.size
                        val audioChunkString = Base64.encodeToString(audio, Base64.DEFAULT)
                        val audioChunkJsonObject = JSONObject()
                            .put("amount", audioChunks.size)
                            .put("number", index + 1) // make it real number
                            .put("buffer", audioChunkString)
                            .put("read_size", readSize)
                        val micTestChunkObject = JSONObject().put("microphone_test", audioChunkJsonObject)
                        streamOutput?.writeUTF(micTestChunkObject.toString())
                        streamOutput?.flush()
                    }
                }
            }
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "null")
        }
    }

    private fun sendSignalMessage() {
        val signalJsonArray =
            RfcxComm.getQueryContentProvider(
                "admin",
                "signal",
                "signal",
                context?.contentResolver
            )
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
            } catch (e: Exception) {
                RfcxLog.logExc(LOGTAG, e)
                verifySocketError(e.message ?: "null")
            }
        }
    }

    private fun sendSyncConfigurationMessage(syncJSONArray: JSONArray) {
        var prefResponse = JSONArray()
        var syncResponse = ""
        try {
            for (i in 0 until syncJSONArray.length()) {
                val pref = syncJSONArray.get(i)
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
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "")
            syncResponse = getSyncResponse("failed")
        } finally {
            streamOutput?.writeUTF(syncResponse)
            streamOutput?.flush()
        }
    }

    fun sendCheckInTestMessage(state: CheckInState, deliveryTime: String? = null) {
        try {
            if (state == CheckInState.NOT_PUBLISHED || requireCheckInTest) {
                val checkInJson = JSONObject()
                    .put("api_url", getFullCheckInUrl())
                    .put("state", state.value)
                    .put("delivery_time", deliveryTime)
                val checkInTestJson = JSONObject()
                    .put("checkin", checkInJson)
                streamOutput?.writeUTF(checkInTestJson.toString())
                streamOutput?.flush()
            }
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "null")
        }
    }

    private fun sendSentinelValues() {
        val sentinelJsonArray =
            RfcxComm.getQueryContentProvider(
                "admin",
                "sentinel_values",
                "sentinel_values",
                context?.contentResolver
            )
        if (sentinelJsonArray.length() > 0) {
            val sentinelJson = sentinelJsonArray.getJSONObject(0)
            try {
                val sentinelInfoJson = JSONObject()
                    .put("sentinel", sentinelJson)
                streamOutput?.writeUTF(sentinelInfoJson.toString())
                streamOutput?.flush()
            } catch (e: Exception) {
                RfcxLog.logExc(LOGTAG, e)
                verifySocketError(e.message ?: "null")
            }
        }
    }

    private fun sendIfGuardianRegistered() {
        val isRegistered = app?.isGuardianRegistered ?: false
        try {
            val isRegisteredJson = JSONObject()
                .put("is_registered", isRegistered)
            streamOutput?.writeUTF(isRegisteredJson.toString())
            streamOutput?.flush()
        } catch (e: Exception) {
            RfcxLog.logExc(LOGTAG, e)
            verifySocketError(e.message ?: "null")
        }
    }

    private fun sendRegistrationStatus(tokenId: String, isProduction: Boolean) {
        val registerJson = JSONObject()
        context?.let {
            val guid = app?.rfcxGuardianIdentity?.guid ?: ""
            RegisterApi.registerGuardian(it, RegisterRequest(guid), tokenId, isProduction, object:
                SocketRegisterCallback {
                override fun onRegisterSuccess(t: Throwable?, response: String?) {
                    try {
                        app?.saveGuardianRegistration(response)
                        val registerInfo = JSONObject()
                            .put("status", "success")
                        registerJson.put("register", registerInfo)
                        streamOutput?.writeUTF(registerJson.toString())
                        streamOutput?.flush()
                    } catch (e: Exception) {
                        RfcxLog.logExc(LOGTAG, e)
                        verifySocketError(e.message ?: "null")
                    }
                }

                override fun onRegisterFailed(t: Throwable?, message: String?) {
                    try {
                        val registerInfo = JSONObject()
                            .put("status", "failed")
                        registerJson.put("register", registerInfo)
                        streamOutput?.writeUTF(registerJson.toString())
                        streamOutput?.flush()
                    } catch (e: Exception) {
                        RfcxLog.logExc(LOGTAG, e)
                        verifySocketError(e.message ?: "null")
                    }
                }
            })
        }
    }

    private fun getFullCheckInUrl(): String {
        val protocol = app?.rfcxPrefs?.getPrefAsString("api_mqtt_protocol") ?: "ssl"
        val host = app?.rfcxPrefs?.getPrefAsString("api_mqtt_host") ?: "api-mqtt.rfcx.org"
        val port = app?.rfcxPrefs?.getPrefAsString("api_mqtt_port") ?: "8883"
        return "$protocol://$host:$port"
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

    private fun verifySocketError(message: String) {
        if (message.contains("null", ignoreCase = true) || message.contains("EPIPE", ignoreCase = true)) {
            if (context != null) {
                Log.d(LOGTAG, "Restart socket service")
                stopServerSocket()
                startServerSocket(context!!)
            }
        }
    }

    private fun ByteArray.toSmallChunk(number: Int): List<ByteArray> {
        val numberOfChunk = this.size / number
        val resultChunk = arrayListOf<ByteArray>()
        var i = 0
        while (i < this.size) {
            resultChunk.add(this.copyOfRange(i, kotlin.math.min(this.size, i + numberOfChunk)))
            i += numberOfChunk
        }
        return resultChunk
    }
  
    enum class CheckInState(val value: String) {
        NOT_PUBLISHED("not published"), PUBLISHING("publishing"), PUBLISHED(
            "published"
        )
    }
}
