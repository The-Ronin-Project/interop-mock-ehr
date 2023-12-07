package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.model.api.Include
import ca.uhn.fhir.rest.annotation.IncludeParam
import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceAndListParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerRoleDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component
import java.security.InvalidParameterException

@Component
class R4PractitionerRoleResourceProvider(
    override var resourceDAO: R4PractitionerRoleDAO,
    // necessary for _include
    private var locationDAO: R4LocationDAO,
    // necessary for _include
    private var practitionerDAO: R4PractitionerDAO,
) : BaseResourceProvider<PractitionerRole, R4PractitionerRoleDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return PractitionerRole::class.java
    }

    @Search
    fun searchByIdentifier(
        @RequiredParam(name = PractitionerRole.SP_IDENTIFIER) idToken: TokenParam,
        @IncludeParam includeSetParam: Set<Include>? = null,
    ): PractitionerRole? {
        val identifier = Identifier()
        identifier.value = idToken.value
        identifier.system = idToken.system
        return resourceDAO.searchByIdentifier(identifier)?.let { handleIncludes(it, includeSetParam) }
    }

    @Search
    fun search(
        @OptionalParam(name = PractitionerRole.SP_LOCATION) locationReferenceParam: ReferenceAndListParam? = null,
        @OptionalParam(name = PractitionerRole.SP_PRACTITIONER) practitionerReferenceParam: ReferenceParam? = null,
        @IncludeParam includeSetParam: Set<Include>? = null,
    ): List<PractitionerRole> {
        val roleList = mutableListOf<PractitionerRole>()
        val locationList = mutableListOf<Reference>()
        var practitionerReference: Reference? = null

        // ReferenceAndListParam is a container for 0..* ReferenceOrListParam, which is in turn a
        // container for 0..* References. It is a little weird to understand at first, but think of the
        // ReferenceAndListParam to be an AND list with multiple OR lists inside it. So we will need
        // to return results which match at least one Reference within every OR list.
        // Note that we are not really supporting ORs inside ANDs, just grabbing the first one
        locationReferenceParam?.valuesAsQueryTokens?.forEach { query ->
            query.valuesAsQueryTokens[0]?.let {
                locationList.add(Reference("Location/${it.value}"))
            }
        }
        practitionerReferenceParam?.let { practitionerReference = Reference("Practitioner/${it.value}") }

        // query for practitionerRoles, iterate through them to add included resources
        resourceDAO.searchByQuery(locationList, practitionerReference).forEach { practitionerRole ->
            roleList.add(handleIncludes(practitionerRole, includeSetParam))
        }
        return roleList
    }

    override fun handleIncludes(
        resource: PractitionerRole,
        includeSet: Set<Include>?,
    ): PractitionerRole {
        includeSet?.forEach {
            when (it.value) {
                "PractitionerRole:location" -> {
                    resource.location =
                        resource.location.mapNotNull { location ->
                            try {
                                location.resource = locationDAO.findById(location.reference.removePrefix("Location/"))
                                location
                            } catch (_: Exception) {
                                // a more robust server would error here,
                                // but we shouldn't enforce reference links for the purposes of this mock EHR
                            }
                        }.filterIsInstance<Reference>() // avoid 'unchecked cast' error
                }

                "PractitionerRole:practitioner" -> {
                    try {
                        resource.practitioner.resource =
                            practitionerDAO.findById(
                                resource.practitioner.reference.removePrefix("Practitioner/"),
                            )
                    } catch (_: Exception) {
                    }
                }

                else -> throw InvalidParameterException("${it.value} is not a supported value for _include.")
            }
        }
        return resource
    }
}
