package org.rfcx.guardian.guardian.entity

data class GuardianResponse (val guid: String, val shortname: String)

fun List<GuardianResponse>.isSuccess(): Boolean {
    return this.isNotEmpty()
}