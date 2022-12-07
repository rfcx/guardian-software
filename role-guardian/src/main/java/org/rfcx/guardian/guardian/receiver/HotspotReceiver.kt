package org.rfcx.guardian.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.rfcx.guardian.utility.misc.ShellCommands

class HotspotReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.toUri(0).toString().contains("component=")) {
            Log.d("Rfcx-hotspot", "guardian")
        }
    }
}
