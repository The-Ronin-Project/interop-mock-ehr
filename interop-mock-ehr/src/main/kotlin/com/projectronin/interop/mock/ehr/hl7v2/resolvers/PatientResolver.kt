package com.projectronin.interop.mock.ehr.hl7v2.resolvers

import ca.uhn.hl7v2.model.v251.datatype.CX
import ca.uhn.hl7v2.model.v251.segment.PID
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import mu.KotlinLogging
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Component

/***
 * Class for resolving HL7v2 components into FHIR Patient objects
 */
@Component
class PatientResolver(private val patientDAO: R4PatientDAO) {
    private val logger = KotlinLogging.logger { }

    /**
     * Given a [pidSegment] attempts to resolve that to a [Patient] already existing in the database
     *
     * pretty naive search, but could be more robust in the future if we find problems
     */
    fun findPatient(pidSegment: PID): Patient? {
        logger.info { "Searching for patient" }
        val patient = pidSegment.patientIdentifierList
            .asSequence()
            .firstNotNullOfOrNull {
                val patientIdentifier = it.toIdentifier()
                logger.debug { "Using identifier with value ${patientIdentifier.value} and system ${patientIdentifier.system}" }
                patientDAO.searchByIdentifier(patientIdentifier)
            }
        return patient
    }

    private fun CX.toIdentifier(): Identifier {
        val identifier = Identifier()
        identifier.value = this.idNumber.value
        identifier.system = this.assigningAuthority.namespaceID.value
        return identifier
    }
}
