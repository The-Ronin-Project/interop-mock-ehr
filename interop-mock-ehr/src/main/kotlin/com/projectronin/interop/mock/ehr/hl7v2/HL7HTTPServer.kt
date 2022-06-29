package com.projectronin.interop.mock.ehr.hl7v2

import ca.uhn.hl7v2.hoh.hapi.server.HohServlet
import com.projectronin.interop.mock.ehr.hl7v2.handlers.MDMReceiverHandler
import org.springframework.stereotype.Component
import javax.servlet.ServletConfig
import javax.servlet.annotation.WebServlet

@WebServlet(urlPatterns = ["/HL7overHTTP/*"])
@Component
class HL7HTTPServer : HohServlet() {
    override fun init(config: ServletConfig) {
        setApplication(MDMReceiverHandler())
    }
}
