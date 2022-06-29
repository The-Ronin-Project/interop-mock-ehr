package com.projectronin.interop.mock.ehr.hl7v2

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.app.SimpleServer
import com.projectronin.interop.mock.ehr.hl7v2.handlers.MDMReceiverHandler
import org.springframework.stereotype.Component

@Component
class HL7TCPServer(port: Int = 1011) {
    final val server: SimpleServer = DefaultHapiContext().newServer(port, false)

    init {
        val mdmHandler = MDMReceiverHandler()
        server.registerApplication("MDM", "T10", mdmHandler)
        server.registerApplication("MDM", "T02", mdmHandler)
        // Add handlers for other message structures here
        server.start()
    }
}
