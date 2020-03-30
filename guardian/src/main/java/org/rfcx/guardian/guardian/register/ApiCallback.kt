package org.rfcx.guardian.guardian.register

interface ApiCallback{
    fun onFailed(t: Throwable?, message: String?)
}