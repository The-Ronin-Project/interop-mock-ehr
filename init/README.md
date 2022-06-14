# interop-mock-ehr/init

## Docker Image

This project produces a docker image that provides portable access to the default data sets contained in the
/init/resources. The image is automatically built off master and distributed into the Ronin docker repo, where it can be
consumed by projects that desire the default set.

### Making Changes

The default content can be updated by interop developers to better suit consumer's needs. New content can be developed
locally and tested by building a local copy of the docker image using the command below.

`docker build -t docker-proxy.devops.projectronin.io/interop-mock-ehr-init:latest .`

## Data Files

Data files under __/init/resources__ support MySql import conventions in a simple way
that manages a large number of similar data files without naming contention.

- There is one FHIR resource per JSON file
- Files meet this naming convention: __/init/resources__/*setName*/*resource*/*fileName*.json
- *setName* can be any value; it can mean anything, such as a set of resources that reference each other
- *resource* is an exact match with the FHIR resource type
- *fileName*.json can be any file name ending in .json
- Example: __/init/resources__/SetB/Appointment/Appointment_ZbQ3.json

When the Mock EHR is started up, the script __/init/init.sh__
automatically loads any files under __/init/resources__
that match these folder and name conventions.
This automation is enabled by docker-compose.yaml volume settings
and MySql import conventions.

This test data is loaded for the Mock EHR on port 8081 in either build scenario:
* a standalone Mock EHR, using interop-mock-ehr, as [here](https://github.com/projectronin/interop-mock-ehr), or
* a full Mirth local development environment that includes the Mock EHR, using interop-mirth-channels, as [here](https://github.com/projectronin/interop-mirth-channels)

### New Data and Data Sets

Before either build scenario for Mock EHR, you may add new data files locally.
You need not create a pull request nor archive the data files in this repo, unless that is your goal.
Any files you add or modify locally are brought into the Mock EHR when you build it.

In your local folders for 
[https://github.com/projectronin/interop-mock-ehr](https://github.com/projectronin/interop-mock-ehr), you may:
* locally add __/init/resources__/*setName*/*resource*/*fileName*.json files, or
* locally edit any __/init/resources__/*setName*/*resource*/*fileName*.json files

When adding new data, follow any conventions you observe in the existing folder structure and file names in this repo.
These conventions enable the __/init/init.sh__ script to find and load your data.

### Data Requirements 

At minimum, the following fields are populated in the test data this repo provides:

- Appointment: Linked to a Patient and Provider
- Condition: Linked to a Patient
- Location: Street, Zip code
- Patient: First Name, Last Name, Gender, Date of Birth, Zip code, Phone number
- Provider: First Name, Last Name
- ProviderRole: Linked to a Location and Provider

Try to meet these data requirements in the new data files you provide.
Otherwise, parts of the Ronin application or some channels may fail unexpectedly, confusing your tests.

### Adding Data vs. Patching Data

Two data operations that you might need are:

* Locally __adding__ data files or data sets before building the Mock EHR. 
  Most of this README describes adding data.


* Locally __patching__ specific fields in the data after it has been built into the Mock EHR.
  One example of patching data is to make all the dates in Appointment resources "recent" relative to today.
  This can be accomplished quickly with a MySql or REST API call to the Mock EHR once it is running.
  Patch techniques are described in "Test Data" and "Development Only" [here](https://github.com/projectronin/interop-mirth-channels).

## Existing Data Sets

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
- and three appointments (ePjxkyjA8gju08Vwqc.iiAAqNojpT2hDqLoM1pkTF0Rw3, ePjxkyjA8gju08Vwqc.iiAFHBGCmkucuk3O15LOr0KFg3,
  eWLhfjXHp4RUczvtT2om.1Ii2uisGHcDc6rMEjO0xHBA3),
- each appointment at a different location (e-ihfN9W6NFF9fqHMGzRyBw3, e6gRswU5WJtj7msgU7NZiYw3,
  e4W4rmGe9QzuGm2Dy4NBqVc0KDe6yGld6HW95UuN-Qd03),
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

