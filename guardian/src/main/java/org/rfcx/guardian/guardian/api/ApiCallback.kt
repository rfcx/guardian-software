package org.rfcx.guardian.guardian.api

interface ApiCallback{
    fun onFailed(t: Throwable?, message: String?)
}