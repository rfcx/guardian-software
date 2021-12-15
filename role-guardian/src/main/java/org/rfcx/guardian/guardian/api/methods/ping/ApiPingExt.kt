package org.rfcx.guardian.guardian.api.methods.ping

import org.json.JSONObject

object ApiPingExt {

    fun shortenPingJson(ping: JSONObject): String {
        val shortenJson = JSONObject()
        ping.keys().forEach { it ->
            when(it) {
                "data_transfer" -> shortenJson.put("data_transfer", ping.getString("data_transfer"))
                "measured_at" -> shortenJson.put("measured_at", ping.getString("measured_at"))
                "cpu" -> shortenJson.put("cpu", ping.getString("cpu"))
                "prefs" -> shortenJson.put("prefs", ping.getString("prefs"))
                "broker_connections" -> shortenJson.put("broker_connections", ping.getString("broker_connections"))
                "meta_ids" -> shortenJson.put("meta_ids", ping.getString("meta_ids"))
                "instructions" -> shortenJson.put("instructions", ping.getString("instructions"))
                "library" -> shortenJson.put("library", ping.getString("library"))
                "messages" -> shortenJson.put("messages", ping.getString("messages"))
                "sentinel_sensor" -> shortenJson.put("sentinel_sensor", ping.getString("sentinel_sensor"))
                "purged" -> {
                    val purged = ping.getString("purged")
                    val shortenPurged = purged.apply {
                        replace("meta", "m")
                        replace("audio", "a")
                    }
                    shortenJson.put("purged", shortenPurged)
                }
                "checkins" -> {
                    val checkins = ping.getString("checkins")
                    val shortenCheckins = checkins.apply {
                        replace("sent", "s")
                        replace("queued", "q")
                        replace("meta", "m")
                        replace("skipped", "sk")
                        replace("stashed", "st")
                        replace("archived", "a")
                        replace("vault", "v")
                    }
                    shortenJson.put("checkins", shortenCheckins)
                }
                "battery" -> {
                    val battery = ping.getString("battery")
                    val batteryAsList = battery.split("|").map { batt -> batt.split("*") }
                    val removedDupBattery = arrayListOf<List<String>>()
                    batteryAsList.forEachIndexed { index, list ->
                        if (index == 0) removedDupBattery.add(list)
                        if (list[index][1] != list[index-1][1]) removedDupBattery.add(list)
                    }
                    val removedDupBatteryString =
                        removedDupBattery.joinToString("|") { it.joinToString("*") }
                    shortenJson.put("battery", removedDupBatteryString)
                }
                "memory" -> {
                    val memory = ping.getString("memory")
                    val shortenMemory = memory.apply {
                        replace("system", "s")
                    }
                    shortenJson.put("memory", shortenMemory)
                }
                //TODO: check if we really need to shorten this property
                "network" -> {
                    val network = ping.getString("network")
                    val networkAsList = network.split("|").map { batt -> batt.split("*") }
                    val removedDupNetwork = arrayListOf<List<String>>()
                    networkAsList.forEachIndexed { index, list ->
                        if (index == 0) removedDupNetwork.add(list)
                        if (list[index][1] != list[index-1][1]) removedDupNetwork.add(list)
                    }
                    val removedDupNetworkString =
                        removedDupNetwork.joinToString("|") { it.joinToString("*") }
                    shortenJson.put("network", removedDupNetworkString)
                }
                "storage" -> {
                    val storage = ping.getString("storage")
                    val shortenStorage = storage.apply {
                        replace("internal", "n")
                        replace("external", "e")
                    }
                    shortenJson.put("storage", shortenStorage)
                }
                "software" -> {
                    val software = ping.getString("software")
                    val shortenSoftware = software.apply {
                        replace("admin", "a")
                        replace("classify", "c")
                        replace("guardian", "g")
                        replace("updater", "u")
                    }
                    shortenJson.put("software", shortenSoftware)
                }
                //TODO: since I cannot use sentinel board to get these data, need to wait for real data
                "sentinel_power" -> {
                    val sentinelPower = ping.getString("sentinel_power")
                    val shortenSentinelPower = sentinelPower.apply {
                        replace("system", "s")
                        replace("battery", "b")
                        replace("input", "i")
                    }
                    shortenJson.put("sentinel_power", shortenSentinelPower)
                }
                "device" -> {
                    val device = ping.getJSONObject("device")

                    val android = device.getJSONObject("android")
                    val shortenAndroid = JSONObject().apply {
                        put("p", android.getString("product"))
                        put("br", android.getString("brand"))
                        put("m", android.getString("model"))
                        put("bu", android.getString("build"))
                        put("a", android.getString("android"))
                        put("m", android.getString("manufacturer"))
                    }

                    val phone = device.getJSONObject("phone")
                    val shortenPhone = JSONObject().apply {
                        put("s", phone.getString("sim"))
                        put("n", phone.getString("number"))
                        put("imei", phone.getString("imei"))
                        put("imsi", phone.getString("imsi"))
                    }

                    val hardware = device.getJSONObject("hardware")
                    val shortenHardware = JSONObject().apply {
                        put("p", hardware.getString("product"))
                        put("br", hardware.getString("brand"))
                        put("m", hardware.getString("model"))
                        put("bu", hardware.getString("build"))
                        put("a", hardware.getString("android"))
                        put("m", hardware.getString("manufacturer"))
                    }

                    val shortenDevice = JSONObject().apply {
                        put("a", shortenAndroid)
                        put("p", shortenHardware)
                        put("h", shortenHardware)
                    }

                    shortenJson.put("device", shortenDevice)
                }
                "detections" -> {
                    
                }
            }
        }
        return ping.toString()
    }
}
