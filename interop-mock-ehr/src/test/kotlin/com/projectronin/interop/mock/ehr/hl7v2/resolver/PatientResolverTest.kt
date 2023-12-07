package com.projectronin.interop.mock.ehr.hl7v2.resolver

import ca.uhn.hl7v2.model.v251.datatype.CX
import ca.uhn.hl7v2.model.v251.segment.PID
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.PatientResolver
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PatientResolverTest {
    private lateinit var patientDAO: R4PatientDAO

    @BeforeEach
    fun init() {
        patientDAO = mockk()
    }

    @Test
    fun `findPatient - can resolve`() {
        val resolver = PatientResolver(patientDAO)
        every { patientDAO.searchByIdentifier(match { it.value == "123" }) } returns Patient()
        every { patientDAO.searchByIdentifier(match { it.value != "123" }) } returns null
        val cx1 =
            mockk<CX> {
                every { idNumber } returns
                    mockk {
                        every { value } returns "123"
                    }
                every { assigningAuthority } returns
                    mockk {
                        every { namespaceID } returns
                            mockk {
                                every { value } returns "MRN"
                            }
                    }
            }
        val cx2 =
            mockk<CX> {
            }

        val pid =
            mockk<PID> {
                every { patientIdentifierList } returns arrayOf(cx1, cx2)
            }
        assertNotNull(resolver.findPatient(pid))
    }

    @Test
    fun `findPatient - can return null`() {
        val resolver = PatientResolver(patientDAO)
        every { patientDAO.searchByIdentifier(any()) } returns null
        val cx1 =
            mockk<CX> {
                every { idNumber } returns
                    mockk {
                        every { value } returns "123"
                    }
                every { assigningAuthority } returns
                    mockk {
                        every { namespaceID } returns
                            mockk {
                                every { value } returns "MRN"
                            }
                    }
            }
        val pid =
            mockk<PID> {
                every { patientIdentifierList } returns arrayOf(cx1)
            }
        assertNull(resolver.findPatient(pid))
    }
}
