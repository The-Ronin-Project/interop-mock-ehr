package com.projectronin.interop.mock.ehr.hl7v2.handlers

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.MDM_T02
import ca.uhn.hl7v2.protocol.ReceivingApplication

/*
    Handles HL7v2.5.1 MDM-style messages. Note that MDM_T02 is the structure of the message, not necessarily the event type.
    An MDM_T02 structured messaged can be a T02, T06, T10, etc. event.
 */
class MDMReceiverHandler : ReceivingApplication<MDM_T02> {

    /*
        'theMessage' is a HAPI Message object with many convenience functions for retrieving data from the HL7v2 structure.
     */
    override fun processMessage(theMessage: MDM_T02, theMetadata: MutableMap<String, Any>?): Message {
        return theMessage.generateACK()
    }

    override fun canProcess(theMessage: MDM_T02?): Boolean {
        return true // it's unclear why we would ever return false here.
    }
}
