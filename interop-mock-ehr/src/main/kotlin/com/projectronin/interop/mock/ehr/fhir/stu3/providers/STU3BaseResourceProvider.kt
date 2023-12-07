package com.projectronin.interop.mock.ehr.fhir.stu3.providers

import ca.uhn.fhir.rest.annotation.Create
import ca.uhn.fhir.rest.annotation.Delete
import ca.uhn.fhir.rest.annotation.IdParam
import ca.uhn.fhir.rest.annotation.Read
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.ResourceParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.annotation.Update
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.server.IResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.dao.BaseResourceDAO
import com.projectronin.interop.mock.ehr.fhir.stu3.toDSTU3
import com.projectronin.interop.mock.ehr.fhir.stu3.toR4
import org.hl7.fhir.dstu3.model.IdType
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.r4.model.Resource as R4Resource

abstract class STU3BaseResourceProvider<T : Resource, R4 : R4Resource, DAO : BaseResourceDAO<R4>> : IResourceProvider {
    abstract var resourceDAO: DAO

    @Read // ex. /fhir/stu3/Patient/123
    fun read(
        @IdParam theId: IdType,
    ): T {
        return (resourceDAO.findById(theId.idPart)).toDSTU3()
    }

    @Search // ex. /fhir/r4/Patient?_id=123,456
    fun readMultiple(
        @RequiredParam(name = Resource.SP_RES_ID) idList: TokenOrListParam,
    ): List<T> {
        return idList.valuesAsQueryTokens.map { resourceDAO.findById(it.value).toDSTU3() }
    }

    @Update
    fun update(
        @IdParam theId: IdType,
        @ResourceParam theResource: T,
    ): MethodOutcome {
        theResource.id = theId.idPart
        resourceDAO.update(theResource.toR4())
        return MethodOutcome().setCreated(true)
    }

    @Update
    fun updateNoId(
        @ResourceParam theResource: T,
    ): MethodOutcome {
        resourceDAO.update(theResource.toR4())
        return MethodOutcome().setCreated(true)
    }

    @Create
    fun create(
        @ResourceParam theResource: T,
    ): MethodOutcome {
        return MethodOutcome().setCreated(true)
            .setId(IdType(resourceDAO.insert(theResource.toR4()))) // return the resource FHIR ID for reference
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
        return resourceDAO.getAll().map { it.toDSTU3() }
    }
}
