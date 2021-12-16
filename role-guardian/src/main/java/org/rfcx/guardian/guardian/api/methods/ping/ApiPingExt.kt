package org.rfcx.guardian.guardian.api.methods.ping

import org.json.JSONObject

object ApiPingExt {

    fun shortenPingJson(ping: JSONObject): JSONObject {
        val shortenJson = JSONObject()
        ping.keys().forEach { it ->
            when(it) {
                "data_transfer" -> shortenJson.put("dt", ping.getString("data_transfer"))
                "measured_at" -> shortenJson.put("ma", ping.getLong("measured_at"))
                "cpu" -> shortenJson.put("cpu", ping.getString("cpu"))
                "prefs" -> shortenJson.put("pfs", ping.getString("prefs"))
                "broker_connections" -> shortenJson.put("bc", ping.getString("broker_connections"))
                "meta_ids" -> shortenJson.put("mtid", ping.getString("meta_ids"))
                "instructions" -> shortenJson.put("inst", ping.getString("instructions"))
                "library" -> shortenJson.put("lib", ping.getString("library"))
                "messages" -> shortenJson.put("msg", ping.getString("messages"))
                "sentinel_sensor" -> shortenJson.put("ss", ping.getString("sentinel_sensor"))
                "swm" -> shortenJson.put("swm", ping.getString("swm"))
                "purged" -> {
                    val purged = ping.getString("purged")
                    val shortenPurged = purged.let {
                        it.replace("meta", "m")
                            .replace("audio", "a")
                    }
                    shortenJson.put("pg", shortenPurged)
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
                                if (list[index][1] != list[index-1][1]) removedDupBattery.add(list)
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
                //TODO: check if we really need to shorten this property
                "network" -> {
                    val network = ping.getString("network")
                    val networkAsList = network.split("|").map { batt -> batt.split("*") }
                    val removedDupNetwork = arrayListOf<List<String>>()
                    networkAsList.forEachIndexed { index, list ->
                        if (networkAsList.size == 1) {
                            removedDupNetwork.add(list)
                        } else {
                            if (index == 0) {
                                removedDupNetwork.add(list)
                            } else {
                                if (list[index][1] != list[index-1][1]) removedDupNetwork.add(list)
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
                //TODO: since I cannot use sentinel board to get these data, need to wait for real data
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

                    val android = device.getJSONObject("android")
                    val shortenAndroid = JSONObject().apply {
                        put("p", android.getString("product"))
                        put("br", android.getString("brand"))
                        put("m", android.getString("model"))
                        put("bu", android.getString("build"))
                        put("a", android.getString("android"))
                        put("mf", android.getString("manufacturer"))
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
                        put("mf", hardware.getString("manufacturer"))
                    }

                    val shortenDevice = JSONObject().apply {
                        put("a", shortenAndroid)
                        put("p", shortenPhone)
                        put("h", shortenHardware)
                    }

                    shortenJson.put("dv", shortenDevice)
                }
                "detections" -> {
                    val detections = ping.getString("detections")
                    val detectionsAsList = detections.split("|").map { detection -> detection.split("*").toMutableList() }.toMutableList()
                    detectionsAsList.forEachIndexed { indexOfDetection, dt ->
                        val shortenConfidence = arrayListOf<String>()
                        val confidence = dt[4].split(",")
                        var emptyCount = 0
                        for (index in confidence.indices) {
                            if (index == confidence.size - 1) {
                                if (confidence[index] == "") {
                                    emptyCount++
                                    shortenConfidence.add("n$emptyCount")
                                } else {
                                    shortenConfidence.add(confidence[index])
                                }
                                break
                            }

                            if (confidence[index] == "" && confidence[index] == confidence[index + 1]) {
                                emptyCount++
                            } else if (confidence[index] == "" && confidence[index] != confidence[index + 1]) {
                                emptyCount++
                                shortenConfidence.add("n$emptyCount")
                                emptyCount = 0
                            } else {
                                shortenConfidence.add(confidence[index])
                            }
                        }
                        detectionsAsList[indexOfDetection][4] = shortenConfidence.joinToString(",")
                    }
                    val removedCommaDetectionsString =
                        detectionsAsList.joinToString("|") { it.joinToString("*") }
                    shortenJson.put("dtt", removedCommaDetectionsString)
                }
            }
        }
        return shortenJson
    }
}
