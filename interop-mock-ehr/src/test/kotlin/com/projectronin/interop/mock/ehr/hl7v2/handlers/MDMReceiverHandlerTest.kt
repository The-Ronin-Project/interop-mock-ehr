package com.projectronin.interop.mock.ehr.hl7v2.handlers

import ca.uhn.hl7v2.model.v251.message.MDM_T02
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MDMReceiverHandlerTest {

    private val mdmHandler = MDMReceiverHandler()
    private val message = mockk<MDM_T02>()

    @Test
    fun `can process messages`() {
        assertTrue(mdmHandler.canProcess(message))
    }

    @Test
    fun `ack test`() {
        every { message.generateACK() } returns message
        val ack = mdmHandler.processMessage(message, mutableMapOf())
        assertEquals(ack, message)
    }
}
