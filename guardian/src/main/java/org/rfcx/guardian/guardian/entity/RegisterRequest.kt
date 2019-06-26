package org.rfcx.guardian.guardian.entity

import com.google.gson.annotations.SerializedName

data class RegisterRequest (
    val guid: String,
    val token: String)