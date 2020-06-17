package org.rfcx.guardian.admin.device.android.wifi

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.rfcx.guardian.admin.RfcxGuardian
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

                serverSocket = ServerSocket(9999)

                while (true) {
                    socket = serverSocket.accept()

                    streamInput = DataInputStream(socket.getInputStream())

                    val data = streamInput.readUTF()

                    if (!data.isNullOrBlank()) {
                        Log.d("ServerSocket", "Receiving data from Client: $data")

                        val receiveJson = JSONObject(data)
                        val jsonIterator = receiveJson.keys()
                        jsonIterator.next()

                        //send response back
                        val streamOut = DataOutputStream(socket.getOutputStream())
                        when (receiveJson.get("command")) {
                            "prefs" -> {
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
                                        streamOut.writeUTF(jsonObject.toString())
                                        streamOut.flush()
                                    }
                                } catch (e: JSONException) {
                                    Log.e(LOGTAG, e.toString())
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
}
