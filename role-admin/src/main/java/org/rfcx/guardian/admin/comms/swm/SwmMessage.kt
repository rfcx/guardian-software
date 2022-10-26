package org.rfcx.guardian.admin.comms.swm

import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.utility.device.DeviceSmsUtils

class SwmMessage(private val app: RfcxGuardian, private val api: SwmApi, private val device: SwmDevice) {

    fun getUnsentMessagesCount(): Int {
        val count = api.getNumberOfUnsentMessages()
        device.awake()
        return count
    }
    fun queueMessageToSwarm(message: String, priority: Int): String? {
        val messageId = api.transmitData(message, priority)
        device.awake()
        return messageId
    }

    fun addScheduledSwmToQueue(
        sendAtOrAfter: Long,
        groupId: String?,
        msgPayload: String?,
        priority: Int,
        triggerDispatchService: Boolean
    ) {
        if (msgPayload != null && !msgPayload.equals("", ignoreCase = true)) {
            val msgId = DeviceSmsUtils.generateMessageId()
            app.swmMessageDb.dbSwmQueued.insert(
                sendAtOrAfter,
                "",
                msgPayload,
                groupId,
                msgId,
                priority
            )
            if (triggerDispatchService) {
                app.rfcxSvc.triggerService(SwmDispatchCycleService.SERVICE_NAME, false)
            }
        }
    }
}
