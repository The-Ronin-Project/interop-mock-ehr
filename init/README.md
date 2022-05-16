# interop-mock-ehr/init

## Data Files

Data files under __/init/resources__ support MySql import conventions in a simple way 
that manages a large number of similar data files without naming contention. 

- There is one FHIR resource per JSON file
- Files meet this naming convention: __/init/resources__/*setName*/*resource*/*fileName*.json
- *setName* can be any value; it can mean anything, such as a set of resources that reference each other
- *resource* is an exact match with the FHIR resource type
- *fileName*.json can be any file name ending in .json
- Example: __/init/resources__/SetB/Appointment/Appointment_ZbQ3.json

When the MockEHR is started up the script __/init/init.sh__
automatically loads any files under __/init/resources__
that match these folder and name conventions.
This automation is enabled by docker-compose.yaml volume settings 
and MySql import conventions.

You may add files or modify the files provided.

## Data Requirements

At minimum, the following fields are populated:

- Appointment: Linked to a Patient and Provider
- Condition: Linked to a Patient
- Location: Street, Zip code
- Patient: First Name, Last Name, Gender, Date of Birth, Zip code, Phone number
- Provider: First Name, Last Name
- ProviderRole: Linked to a Location and Provider

## Data Summary

### In SetA

Patient (eJzlzKe3KPzAV5TtkxmNivQ3)
- with two appointments (06d7feb3-3326-4276-9535-83a622d8e217, 06d7feb3-3326-4276-9535-83a622d8e216)
- each with the same condition (39bb2850-50e2-4bb0-a5ae-3a98bbaf199f),
- location (3f1af7cb-a47e-4e1e-a8e3-d18e0d073e6c),
- and practitioner (06d7feb3-3326-4276-9535-83a622d8e215).
- Also included is a practitioner role (06d7feb3-3326-4276-9535-83a622d8e226).

### In SetB

Patient (eFs2zvgmbGfg-12742400)
- with two conditions (p109117485, 00a5d6eb-c567-42f7-be07-53804cece075)
- with one appointment (ePjxkyjA8gju08Vwqc.iiAPwx9VIfkAmBkO0QzHl0ZbQ3)
- at location (e6gRswU5WJtj7msgU7NZiYw3)
- with practitioner (euc69RmkeUC5UjZOIGu0FiA3).
- Also included is the practitioner role (06d7feb3-3326-4276-9535-83a622d8e221).

Patient (eJzlzKe3KPzAV5TtkxmNivQ3)
- with one condition (39bb2850-50e2-4bb0-a5ae-3a98bbaf0001)
- and three appointments (ePjxkyjA8gju08Vwqc.iiAAqNojpT2hDqLoM1pkTF0Rw3, ePjxkyjA8gju08Vwqc.iiAFHBGCmkucuk3O15LOr0KFg3, eWLhfjXHp4RUczvtT2om.1Ii2uisGHcDc6rMEjO0xHBA3),
- each appointment at a different location (e-ihfN9W6NFF9fqHMGzRyBw3, e6gRswU5WJtj7msgU7NZiYw3, e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03),
- with a different practitioner (ejTHcJyAyLP9ittoMQVgIFg3, euc69RmkeUC5UjZOIGu0FiA3, eM5CWtq15N0WJeuCet5bJlQ3).
- One of the appointments (ePjxkyjA8gju08Vwqc.iiAFHBGCmkucuk3O15LOr0KFg3) is about the condition.
- Also included are practitioner roles (06d7feb3-3326-4276-9535-83a622d8e222, 06d7feb3-3326-4276-9535-83a622d8e223).

One of the practitioners (euc69RmkeUC5UjZOIGu0FiA3) sees both patients.

### In SetC

The appointments have various codes: CHECKUP, WALKIN, EMERGENCY, ROUTINE.

There is one location (EHRFHIRIDLocation01Test) for all appointments and practitioner roles.

Patient (EHRFHIRIDPatient01Test)
- has one appointment (EHRFHIRIDAppointment01Test)
- with one practitioner (EHRFHIRIDPractitioner01Test).

Patient (EHRFHIRIDPatient02Test)
- has one appointment (EHRFHIRIDAppointment02Test)
- with one practitioner (EHRFHIRIDPractitioner02Test).

Patient (EHRFHIRIDPatient03Test)
- has one appointment (EHRFHIRIDAppointment03Test)
- with one practitioner (EHRFHIRIDPractitioner03Test).

Patient (EHRFHIRIDPatient04Test)
- has one appointment (EHRFHIRIDAppointment04Test)
- with one practitioner (EHRFHIRIDPractitioner04Test).

Patient (EHRFHIRIDPatient05Test)
- has one appointment (EHRFHIRIDAppointment05Test)
- with one practitioner (EHRFHIRIDPractitioner05Test).

