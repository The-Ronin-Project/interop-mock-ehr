package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.model.api.Include
import ca.uhn.fhir.rest.annotation.Create
import ca.uhn.fhir.rest.annotation.Delete
import ca.uhn.fhir.rest.annotation.IdParam
import ca.uhn.fhir.rest.annotation.IncludeParam
import ca.uhn.fhir.rest.annotation.Patch
import ca.uhn.fhir.rest.annotation.Read
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.ResourceParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.annotation.Update
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.PatchTypeEnum
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.server.IResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.dao.BaseResourceDAO
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Resource
import java.security.InvalidParameterException

abstract class BaseResourceProvider<T : Resource, DAO : BaseResourceDAO<T>> : IResourceProvider {
    abstract var resourceDAO: DAO

    @Read // ex. /fhir/r4/Patient/123
    fun read(
        @IdParam theId: IdType,
    ): T {
        return resourceDAO.findById(theId.idPart)
    }

    @Search // ex. /fhir/r4/Patient?_id=123,456
    fun readMultiple(
        @RequiredParam(name = Resource.SP_RES_ID) idList: TokenOrListParam,
    ): List<T> {
        return idList.valuesAsQueryTokens.map { (resourceDAO.findById(it.value)) }
    }

    @Search // ex. /fhir/r4/Patient?_id=123&_include=Patient:managingOrganization
    fun readWithIncludes(
        @RequiredParam(name = Resource.SP_RES_ID) theId: IdType,
        @IncludeParam includeSetParam: Set<Include>?,
    ): T {
        return handleIncludes(resourceDAO.findById(theId.idPart), includeSetParam)
    }

    @Update
    fun update(
        @IdParam theId: IdType,
        @ResourceParam theResource: T,
    ): MethodOutcome {
        theResource.id = theId.idPart
        resourceDAO.update(theResource)
        return MethodOutcome().setCreated(true)
    }

    @Update
    fun updateNoId(
        @ResourceParam theResource: T,
    ): MethodOutcome {
        resourceDAO.update(theResource)
        return MethodOutcome().setCreated(true)
    }

    @Create
    fun create(
        @ResourceParam theResource: T,
    ): MethodOutcome {
        return MethodOutcome().setCreated(true)
            .setId(IdType(resourceDAO.insert(theResource))) // return the resource FHIR ID for reference
    }

    @Delete
    fun delete(
        @IdParam theId: IdType,
    ): MethodOutcome {
        resourceDAO.delete(theId.idPart)
        return MethodOutcome().setCreated(false)
    }

    @Search
    fun returnAll(): List<T> {
        return resourceDAO.getAll()
    }

    @Patch
    fun patch(
        @IdParam theID: IdType,
        patchType: PatchTypeEnum,
        @ResourceParam rawPatch: String,
    ): MethodOutcome {
        if (patchType != PatchTypeEnum.JSON_PATCH) {
            throw java.lang.UnsupportedOperationException("Only JSON patch types allowed.")
        }
        resourceDAO.patch(theID.idPart, rawPatch)
        return MethodOutcome()
    }

    // implementing functions should add this when necessary, but not required by default.
    open fun handleIncludes(
        resource: T,
        includeSet: Set<Include>?,
    ): T {
        if (includeSet?.isNotEmpty() == true) {
            throw InvalidParameterException("'_include' parameters are not implemented for this resource type.")
        }
        return resource
    }
}
