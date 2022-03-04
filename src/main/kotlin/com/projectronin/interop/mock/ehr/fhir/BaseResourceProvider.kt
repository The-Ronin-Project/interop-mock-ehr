package com.projectronin.interop.mock.ehr.fhir

import ca.uhn.fhir.rest.annotation.Create
import ca.uhn.fhir.rest.annotation.Delete
import ca.uhn.fhir.rest.annotation.IdParam
import ca.uhn.fhir.rest.annotation.Read
import ca.uhn.fhir.rest.annotation.ResourceParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.annotation.Update
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.server.IResourceProvider
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Resource

abstract class BaseResourceProvider<T : Resource, DAO : BaseResourceDAO<T>> : IResourceProvider {

    abstract var resourceDAO: DAO

    @Read
    fun read(@IdParam theId: IdType): T {
        return resourceDAO.findById(theId.idPart)
    }

    @Update
    fun update(@IdParam theId: IdType, @ResourceParam theResource: T): MethodOutcome {
        theResource.id = theId.idPart
        resourceDAO.update(theResource)
        return MethodOutcome().setCreated(true)
    }

    @Update
    fun updateNoId(@ResourceParam theResource: T): MethodOutcome {
        resourceDAO.update(theResource)
        return MethodOutcome().setCreated(true)
    }

    @Create
    fun create(@ResourceParam theResource: T): MethodOutcome {
        return MethodOutcome().setCreated(true)
            .setId(IdType(resourceDAO.insert(theResource))) // return the resource FHIR ID for reference
    }

    @Delete
    fun delete(@IdParam theId: IdType): MethodOutcome {
        resourceDAO.delete(theId.idPart)
        return MethodOutcome().setCreated(false)
    }

    @Search
    fun returnAll(): List<T> {
        return resourceDAO.getAll()
    }
}
