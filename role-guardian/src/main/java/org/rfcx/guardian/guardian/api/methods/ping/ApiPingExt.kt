package org.rfcx.guardian.guardian.api.methods.ping

import org.json.JSONObject

object ApiPingExt {

    fun shortenPingJson(ping: JSONObject): JSONObject {
        val shortenJson = JSONObject()
        ping.keys().forEach { it ->
            when (it) {
                "data_transfer" -> shortenJson.put("dt", ping.get("data_transfer"))
                "measured_at" -> shortenJson.put("ma", ping.get("measured_at"))
                "cpu" -> shortenJson.put("cpu", ping.get("cpu"))
                "broker_connections" -> shortenJson.put("bc", ping.get("broker_connections"))
                "meta_ids" -> shortenJson.put("mid", ping.get("meta_ids"))
                "detection_ids" -> shortenJson.put("did", ping.get("detection_ids"))
                "instructions" -> shortenJson.put("instructions", ping.get("instructions"))
                "library" -> shortenJson.put("lib", ping.get("library"))
                "messages" -> shortenJson.put("msg", ping.get("messages"))
                //TODO: bme688 -> bm & ss -> s & reduce decimal place
                "sentinel_sensor" -> shortenJson.put("ss", ping.get("sentinel_sensor"))
                "swm" -> shortenJson.put("swm", ping.get("swm"))
                "purged" -> shortenJson.put("p", ping.get("purged"))
                "prefs" -> {
                    val prefs = ping.getJSONObject("prefs")
                    val shortenPrefs = JSONObject()

                    prefs.keys().forEach { key ->
                        when (key) {
                            "sha1" -> {
                                val sha1 = prefs.getString("sha1")
                                shortenPrefs.put("s", sha1)
                            }
                            "vals" -> {
                                val vals = prefs.getJSONObject("vals")
                                shortenPrefs.put("v", vals)
                            }
                        }
                    }

                    shortenJson.put("pf", shortenPrefs)
                }
                "checkins" -> {
                    val checkins = ping.getString("checkins")
                    val shortenCheckins = checkins.let {
                        it.replace("sent", "s")
                            .replace("queued", "q")
                            .replace("meta", "m")
                            .replace("skipped", "sk")
                            .replace("stashed", "st")
                            .replace("archived", "a")
                            .replace("vault", "v")
                    }
                    shortenJson.put("chn", shortenCheckins)
                }
                "battery" -> {
                    val battery = ping.getString("battery")
                    val batteryAsList = battery.split("|").map { batt -> batt.split("*") }
                    val removedDupBattery = arrayListOf<List<String>>()
                    batteryAsList.forEachIndexed { index, list ->
                        if (batteryAsList.size == 1) {
                            removedDupBattery.add(list)
                        } else {
                            if (index == 0) {
                                removedDupBattery.add(list)
                            } else {
                                if (batteryAsList[index][1] != batteryAsList[index - 1][1]) removedDupBattery.add(
                                    list
                                )
                            }
                        }
                    }
                    val removedDupBatteryString =
                        removedDupBattery.joinToString("|") { it.joinToString("*") }
                    shortenJson.put("btt", removedDupBatteryString)
                }
                "memory" -> {
                    val memory = ping.getString("memory")
                    val shortenMemory = memory.let {
                        it.replace("system", "s")
                    }
                    shortenJson.put("mm", shortenMemory)
                }
                "network" -> {
                    val network = ping.getString("network")
                    val networkAsList = network.split("|").map { nw -> nw.split("*") }
                    val removedDupNetwork = arrayListOf<List<String>>()
                    networkAsList.forEachIndexed { index, list ->
                        if (networkAsList.size == 1) {
                            removedDupNetwork.add(list)
                        } else {
                            if (index == 0) {
                                removedDupNetwork.add(list)
                            } else {
                                if (networkAsList[index][1] != networkAsList[index - 1][1]) removedDupNetwork.add(
                                    list
                                )
                            }
                        }
                    }
                    val removedDupNetworkString =
                        removedDupNetwork.joinToString("|") { it.joinToString("*") }
                    shortenJson.put("nw", removedDupNetworkString)
                }
                "storage" -> {
                    val storage = ping.getString("storage")
                    val shortenStorage = storage.let {
                        it.replace("internal", "i")
                            .replace("external", "e")
                    }
                    shortenJson.put("str", shortenStorage)
                }
                "software" -> {
                    val software = ping.getString("software")
                    val shortenSoftware = software.let {
                        it.replace("admin", "a")
                            .replace("classify", "c")
                            .replace("guardian", "g")
                            .replace("updater", "u")
                    }
                    shortenJson.put("sw", shortenSoftware)
                }
                "sentinel_power" -> {
                    val sentinelPower = ping.getString("sentinel_power")
                    val shortenSentinelPower = sentinelPower.let {
                        it.replace("system", "s")
                            .replace("battery", "b")
                            .replace("input", "i")
                    }
                    shortenJson.put("sp", shortenSentinelPower)
                }
                "device" -> {
                    val device = ping.getJSONObject("device")
                    val shortenDevice = JSONObject()

                    device.keys().forEach { dKey ->
                        when (dKey) {
                            "android" -> {
                                val android = device.getJSONObject("android")
                                val androidObj = JSONObject()
                                android.keys().forEach { aKey ->
                                    when (aKey) {
                                        "product" -> androidObj.put(
                                            "p",
                                            android.getString("product")
                                        )
                                        "brand" -> androidObj.put("br", android.getString("brand"))
                                        "model" -> androidObj.put("m", android.getString("model"))
                                        "build" -> androidObj.put("bu", android.getString("build"))
                                        "android" -> androidObj.put(
                                            "a",
                                            android.getString("android")
                                        )
                                        "manufacturer" -> androidObj.put(
                                            "mf",
                                            android.getString("manufacturer")
                                        )
                                    }
                                }
                                shortenDevice.put("a", androidObj)
                            }
                            "phone" -> {
                                val phone = device.getJSONObject("phone")
                                val phoneObj = JSONObject()
                                phone.keys().forEach { pKey ->
                                    when (pKey) {
                                        "sim" -> phoneObj.put("s", phone.getString("sim"))
                                        "number" -> phoneObj.put("n", phone.getString("number"))
                                        "imei" -> phoneObj.put("imei", phone.getString("imei"))
                                        "imsi" -> phoneObj.put("imsi", phone.getString("imsi"))
                                    }
                                }
                                shortenDevice.put("p", phoneObj)
                            }
                        }
                    }

                    shortenJson.put("dv", shortenDevice)
                }
                "detections" -> {
                    val detections = ping.getString("detections")
                    val detectionsAsList = detections.split("|")
                        .map { detection -> detection.split("*").toMutableList() }.toMutableList()
                    detectionsAsList.forEachIndexed { indexOfDetection, dt ->
                        detectionsAsList[indexOfDetection][0] = shortenDetectionType(dt[0])
                        detectionsAsList[indexOfDetection][4] = shortenDetectionConfidence(dt[4])
                    }
                    val removedCommaDetectionsString =
                        detectionsAsList.joinToString("|") { it.joinToString("*") }
                    shortenJson.put("dtt", removedCommaDetectionsString)
                }
                else -> shortenJson.put(it, ping.get(it))
            }
        }
        return shortenJson
    }

    fun shortenDetectionType(type: String): String {
        return when(type) {
            DetectionType.CHAINSAW.fullName -> DetectionType.CHAINSAW.shortenName
            else -> type
        }
    }

    fun shortenDetectionConfidence(confidences: String): String {
        val shortenConfidence = arrayListOf<String>()
        val confidence = confidences.split(",")
        var emptyCount = 0
        for (index in confidence.indices) {
            // Last index
            if (index == confidence.size - 1) {
                // No need null at the end of detections
                if (confidence[index] != "") {
                    shortenConfidence.add(confidence[index])
                }
                break
            }

            // Check if next index is null then increase empty sum and go next iteration
            if (confidence[index] == "" && confidence[index] == confidence[index + 1]) {
                emptyCount++
            // Check if next index is not null then increase and add n{x} to list
            } else if (confidence[index] == "" && confidence[index] != confidence[index + 1]) {
                emptyCount++
                if (emptyCount > 3) {
                    shortenConfidence.add("n$emptyCount")
                } else {
                    for (c in 0 until emptyCount) {
                        shortenConfidence.add(confidence[index])
                    }
                }
                emptyCount = 0
            } else {
                shortenConfidence.add(confidence[index])
            }
        }
        return shortenConfidence.joinToString(",")
    }

    private enum class DetectionType(val fullName: String, val shortenName: String) {
        CHAINSAW("chainsaw", "c")
    }
}
