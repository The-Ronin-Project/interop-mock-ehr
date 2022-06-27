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
- Observation: Status, Category, Code, Subject
- Patient: First Name, Last Name, Gender, Date of Birth, Zip code, Phone number
- Provider: First Name, Last Name
- ProviderRole: Linked to a Location and Provider
- Observation: Linked to a Patient

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

## Test Data Sets

Resources use Epic vendor conventions. 
For example, Patient and Practitioner have typical Epic identifier lists.

### In SetA - One Patient with Related Resources

Patient (eJzlzKe3KPzAV5TtkxmNivQ3)

- with two appointments (06d7feb3-3326-4276-9535-83a622d8e217, 06d7feb3-3326-4276-9535-83a622d8e216)
- each with the same condition (39bb2850-50e2-4bb0-a5ae-3a98bbaf199f),
- location (3f1af7cb-a47e-4e1e-a8e3-d18e0d073e6c),
- and practitioner (06d7feb3-3326-4276-9535-83a622d8e215).
- Also included is a practitioner role (06d7feb3-3326-4276-9535-83a622d8e226),
- and an observation in [category](http://terminology.hl7.org/CodeSystem/observation-category) 
  vital-signs, effective 2016-03-28 (3f1af7cb-a47e-4e1e-a8e3-d18e0d073e6d).

### In SetB - Interrelated Practitioners, Patients, Appointments, Locations, and Practitioner Roles

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

### In SetC - Appointments with Different Codes

The appointments have various codes: CHECKUP, WALKIN, EMERGENCY, ROUTINE.

- There is one location (EHRFHIRIDLocation01Test) for all appointments and practitioner roles.
- Each patient (EHRFHIRIDPatient01Test)
    - has one appointment (EHRFHIRIDAppointment01Test)
    - with one practitioner (EHRFHIRIDPractitioner01Test)
    - with one practitioner role (EHRFHIRIDPractitionerRole01Test).
- ... and so on for 02, 03, 04, 05

### In SetD - Observations with Different Categories

Observation __category__ is not a required field, and has no required valueset, although
the [FHIR Spec](http://hl7.org/fhir/observation-definitions.html#Observation.category) 
suggests a [valueset](http://terminology.hl7.org/CodeSystem/observation-category).
__status__, __code__, and __subject__ are the required fields, but 
__category__ is vital to understanding observations and where they fit into the complex 
[FHIR US Core Profile](https://hl7.org/fhir/us/core/).

This data set:

- Has a patient (example), practitioner (example), and many observations with different category values.
- Leverages sample observation resources from
  [build.fhir.org](http://build.fhir.org) and [Epic App Orchard API](https://apporchard.epic.com/Sandbox) documentation.
- Has observations with the following category, status, effective and issued dates, and FHIR IDs.

category | status | effective | issued | id 
-------- | --------- | -------- | ------ | ---
- | final | - | 2013-04-04T14:34:00+01:00 | egfr-reference-range
core-characteristics | final | - | - | epQLvEY3
exam | final | 2018-04-02T10:30:10+01:00 | 2018-04-03T15:30:10+01:00 | abdo-tender-coded
functional-mental-status | final | 2021-05-04 | - | eK.Ylq43
laboratory | final | 2021-09-02T18:41:00Z | 2021-09-02T19:10:25Z | eAMXUhKX
laboratory | final | 2021-11-18T06:00:00Z | 2021-11-18T16:50:32Z | ey4sw9N3
LDA | final | 2022-01-17T18:30:00Z | - | eFPRgtg3
procedure | final | 2015-02-19T09:30:35+01:00 | - | ekg-sampled-data
smartdata | unknown | - | 2021-04-16T20:56:23Z | eBBGxpY3
smartdata | unknown | - | 2021-04-16T20:56:23Z | e-YNPQo3
smartdata | unknown | - | 2021-04-16T20:56:02Z | e4OG7jU3
smartdata | unknown | - | 2021-04-16T20:56:02Z | eqA2EG83
social-history | final | 2014-12-11T04:44:16Z | - | component-answers
social-history | final | 2021-11-08T06:00:00Z | 2021-11-08T06:00:00Z | eqEP70B3
vital-signs | cancelled | 2012-09-17 | - | blood-pressure-cancel
vital-signs | final | 1999-07-02 | - | vitals-panel
vital-signs | final | 1999-07-02 | - | body-temperature
vital-signs | final | 1999-07-02 | - | blood-pressure
vital-signs | final | 1999-07-02 | - | heart-rate
vital-signs | final | 1999-07-02 | - | respiratory-rate

