package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.EpicDose
import com.projectronin.interop.ehr.epic.EpicMedAdmin
import com.projectronin.interop.ehr.epic.EpicMedAdminRequest
import com.projectronin.interop.ehr.epic.EpicMedicationAdministration
import com.projectronin.interop.ehr.epic.EpicMedicationOrder
import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageResponse
import com.projectronin.interop.ehr.epic.apporchard.model.SetSmartDataValuesRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SetSmartDataValuesResult
import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import com.projectronin.interop.mock.ehr.epic.dal.EpicDAL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.text.SimpleDateFormat
import java.util.UUID

@RestController
@RequestMapping("/epic")
class EpicServer(private var dal: EpicDAL) {
    @Operation(summary = "Returns Mock Epic Authentication Token", description = "Returns token if successful")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = EpicAuthentication::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/oauth2/token")
    fun getAuthToken(): EpicAuthentication {
        return EpicAuthentication(
            accessToken = UUID.randomUUID().toString(),
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search",
        )
    }

    @Operation(
        summary = "Returns Patient Appointments ",
        description = "returns list of epic appointments if successful",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful Operation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetAppointmentsResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request",
                content = [Content(mediaType = "application/text")],
            ),
        ],
    )
    @PostMapping("/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments")
    fun getAppointmentsByPatient(
        @RequestBody request: GetPatientAppointmentsRequest,
    ): GetAppointmentsResponse {
        // start date required
        val start =
            kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.startDate) }
                .getOrElse { return errorResponse("INVALID-START-DATE") }

        // end date not required
        val end =
            request.endDate?.let {
                kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.endDate) }
                    .getOrElse { return errorResponse("INVALID-END-DATE") }
            }
        // more validation
        end?.let { if (start.after(it)) return errorResponse("END-DATE-BEFORE-START-DATE") }
        // dates have milliseconds, but Epic's search doesn't allow for anything more granular than day
        // so if someone passes "5/27" they should get appointments at like "5/27 at 8:00 AM"
        // if you don't do this the search would end at "5/27 12:00 AM"
        end?.hours = 23
        end?.minutes = 59
        if (request.userID == null) return errorResponse("NO-USER-FOUND") // can check this 'for real' later

        // try to find patient, we're expecting an MRN from the client so use mock EHR's internal mrn system
        val patientID = Identifier().setValue(request.patientId).setSystem("mockEHRMRNSystem")
        val patient = dal.r4PatientDAO.searchByIdentifier(patientID) ?: return errorResponse("NO-PATIENT-FOUND")

        // search for appointments
        val r4appointments =
            dal.r4AppointmentDAO.searchByQuery(
                references = listOf(Reference().setReference(patient.id)),
                fromDate = start,
                toDate = end,
            )
        val epicAppointments =
            r4appointments.map { dal.r4AppointmentTransformer.transformToEpicAppointment(it, patient) }
        return GetAppointmentsResponse(appointments = epicAppointments, error = null)
    }

    @Operation(
        summary = "Returns Provider Appointments",
        description = "Returns list of epic appointments if successful",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = GetAppointmentsResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request",
                content = [Content(mediaType = "application/text")],
            ),
        ],
    )
    @PostMapping("/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments")
    fun getAppointmentsByPractitioner(
        @RequestBody request: GetProviderAppointmentRequest,
    ): GetAppointmentsResponse {
        // start date required
        val start =
            kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.startDate) }
                .getOrElse { return errorResponse("INVALID-START-DATE") }

        // end date not required
        val end =
            request.endDate?.let {
                kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.endDate) }
                    .getOrElse { return errorResponse("INVALID-END-DATE") }
            }

        // more validation
        end?.let { if (start.after(it)) return errorResponse("END-DATE-BEFORE-START-DATE") }
        // dates have milliseconds, but Epic's search doesn't allow for anything more granular than day
        // so if someone passes "5/27" they should get appointments at like "5/27 at 8:00 AM"
        // if you don't do this the search would end at "5/27 12:00 AM"
        end?.hours = 23
        end?.minutes = 59
        if (request.userID == null) return errorResponse("NO-USER-FOUND")

        // find practitioners
        val r4Practitioners =
            request.providers?.mapNotNull {
                dal.r4PractitionerDAO.searchByIdentifier(
                    Identifier().setValue(it.id).setType(CodeableConcept().setText(it.idType)),
                )
            } ?: emptyList()

        // use practitioners if we have them
        val r4ReferenceList =
            r4Practitioners.ifEmpty {
                // otherwise use departments
                val r4Departments =
                    request.departments?.mapNotNull {
                        dal.r4LocationDAO.searchByIdentifier(
                            Identifier().setValue(it.id).setSystem("mockEHRDepartmentInternalSystem"),
                        )
                    } ?: emptyList()
                r4Departments.ifEmpty { return errorResponse("NO-PROVIDER-FOUND") }
            }

        // find all appointments for all practitioners
        val epicAppointments = mutableListOf<EpicAppointment>()
        r4ReferenceList.forEach {
            val r4Appointments =
                dal.r4AppointmentDAO.searchByQuery(
                    references = listOf(Reference().setReference(it.id)),
                    fromDate = start,
                    toDate = end,
                )

            r4Appointments.forEach { r4Appointment ->
                val patientId =
                    r4Appointment.participant.find { participant ->
                        participant.actor.reference.contains("Patient")
                    }?.actor?.reference

                val patient = patientId?.let { dal.r4PatientDAO.findById(patientId.removePrefix("Patient/")) }

                epicAppointments.add(dal.r4AppointmentTransformer.transformToEpicAppointment(r4Appointment, patient))
            }
        }
        return GetAppointmentsResponse(appointments = epicAppointments, error = null)
    }

    @Operation(summary = "Send Message", description = "Returns pair of communication ID and FHIR ID if successful")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = SendMessageResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request",
                content = [Content(mediaType = "application/text")],
            ),
        ],
    )
    @PostMapping("/api/epic/2014/Common/Utility/SENDMESSAGE/Message")
    fun createCommunication(
        @RequestBody sendMessageRequest: SendMessageRequest,
    ): SendMessageResponse {
        // validate patient if it exists
        sendMessageRequest.patientID?.let {
            // expecting "MRN" or something similar, so hardcode MockEHR MRN system.
            dal.r4PatientDAO.searchByIdentifier(Identifier().setValue(it).setSystem("mockEHRMRNSystem"))
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID-PATIENT",
                )
        }
        val communication = dal.r4CommunicationTransformer.transformFromSendMessage(sendMessageRequest)
        val newCommunicationId = dal.r4CommunicationDAO.insert(communication)
        return SendMessageResponse(listOf(IDType(id = newCommunicationId, type = "FHIR ID")))
    }

    @Operation(
        summary = "Add onboard flag",
        description = "Adds a smart data element to patient indicating Ronin onboarding",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = SetSmartDataValuesResult::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request",
                content = [Content(mediaType = "application/text")],
            ),
        ],
    )
    @PutMapping("/api/epic/2013/Clinical/Utility/SETSMARTDATAVALUES/SmartData/Values")
    fun addOnboardFlag(
        @RequestBody request: SetSmartDataValuesRequest,
    ): SetSmartDataValuesResult {
        // validate patient if it exists
        // expecting "MRN" or something similar, so hardcode MockEHR MRN system.
        val patient =
            dal.r4PatientDAO.searchByIdentifier(Identifier().setValue(request.id).setSystem("mockEHRMRNSystem"))
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "NO-ENTITY-FOUND details: No entity found for the provided EntityID($request.id) and EntityIDType(mockEHRMRNSystem).",
                )
        val existingFlags = dal.r4FlagDAO.searchByQuery("Patient/${patient.id.removePrefix("Patient/")}")
        // don't create multiple flags
        val flagCode = "mockEHRPatientOnboardedFlag"
        if (existingFlags.any { it.code.text.contains(flagCode) }) {
            return SetSmartDataValuesResult(success = true)
        }
        // add R4 flag indicating onboarding
        val flag = Flag()
        flag.code = CodeableConcept().setText(flagCode)
        flag.subject = Reference().setReference("Patient/${patient.id.removePrefix("Patient/")}")
        dal.r4FlagDAO.insert(flag)
        return SetSmartDataValuesResult(success = true)
    }

    @PostMapping("/api/epic/2014/Clinical/Patient/GETMEDICATIONADMINISTRATIONHISTORY/MedicationAdministration")
    fun getMedicationAdministration(
        @RequestBody request: EpicMedAdminRequest,
    ): EpicMedAdmin {
        val patientID =
            dal.r4PatientDAO.searchByIdentifier(
                Identifier().setValue(request.patientID).setSystem("mockPatientInternalSystem"),
            )?.id?.removePrefix("Patient/") ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "INVALID-PATIENT",
            )
        val medOrders =
            request.orderIDs.map {
                val medRequest =
                    dal.r4MedicationRequestDAO.searchByQuery(
                        subject = "Patient/$patientID",
                        identifier = Identifier().setSystem("mockEHROrderSystem").setValue(it.id),
                    )
                if (medRequest.size != 1) {
                    throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "${medRequest.size} MedicationRequests found for Order ID ${it.id}, expected 1.",
                    )
                }
                val medAdminList =
                    dal.r4MedAdminDAO.searchByRequest(medRequest.first().id.removePrefix("MedicationRequest/"))
                EpicMedicationOrder(
                    name = medAdminList.first().medicationCodeableConcept.text,
                    medicationAdministrations =
                        medAdminList.map { R4MedAdmin ->
                            EpicMedicationAdministration(
                                administrationInstant = R4MedAdmin.effectiveDateTimeType.valueAsString,
                                dose =
                                    EpicDose(
                                        value = R4MedAdmin.dosage.dose.value.toPlainString(),
                                        unit = R4MedAdmin.dosage.dose.unit,
                                    ),
                                action =
                                    when (R4MedAdmin.status.toCode()) {
                                        "completed" -> "Given"
                                        "in-progress" -> "Pending"
                                        "not-done" -> "Missed"
                                        "entered-in-error" -> "Canceled Entry"
                                        "stopped" -> "Stopped"
                                        "on-hold" -> "Held"
                                        else -> "?"
                                    },
                                rate = null,
                            )
                        },
                )
            }
        return EpicMedAdmin(medOrders)
    }
}

// wrapper for throwing 400 - Bad Request responses
private fun errorResponse(msg: String): GetAppointmentsResponse {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
}
