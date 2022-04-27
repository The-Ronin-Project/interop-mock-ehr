# interop-mock-ehr

This is a Spring Boot server which exposes REST APIs for all EHR endpoints
Ronin needs to interact with. Calls to this server should expect identical behavior
to an equivalent EHR counterpart, and data is returned from a MySql database
which Ronin has full control over.

As an example:

HTTP GET on `http://localhost:8080/fhir/Patient/<patient fhir id>` will return an 
R4 Patient resource.


## Running Locally

To set up MySql, run this docker code in a terminal:

```
docker run --name=mock-ehr-mysql -p 33060:33060 -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=mock_ehr_db 
-e MYSQL_USER=springuser -e MYSQL_PASSWORD=ThePassword -d mysql/mysql-server:latest-aarch64
```
You might have to get rid of the -aarch64 at the end if you are not using an M1 Mac.

You will then have to set local environment variables that match the values you set in the docker command:
```
MOCK_EHR_DB_HOST=localhost
MOCK_EHR_DB_PORT=33060
MOCK_EHR_DB_NAME=mock_ehr_db
MOCK_EHR_DB_USER=springuser
MOCK_EHR_DB_PASS=ThePassword
```
To run the server, use the `./gradlew bootRun` command in a terminal located in the root directory.

## Running via Docker Compose

The server itself and the MySQL database can be booted at the same time in a docker container 
using the following command while inside the project root directory:
```
./gradlew bootJar
docker compose build --no-cache && docker compose up --force-recreate
```
The service will be available at port 8080 of your computer. Assuming you downloaded the entire repo to run this, the
server will be initialized with the data in the /init/resources directory. Feel free to modify these files locally.
```
Patient (eJzlzKe3KPzAV5TtkxmNivQ3) 
 - with two appointments (06d7feb3-3326-4276-9535-83a622d8e217, 06d7feb3-3326-4276-9535-83a622d8e216) 
 - each with the same condition (39bb2850-50e2-4bb0-a5ae-3a98bbaf199f), 
 - location (3f1af7cb-a47e-4e1e-a8e3-d18e0d073e6c), 
 - and practitioner (06d7feb3-3326-4276-9535-83a622d8e216). 
Also included is a practitioner role (06d7feb3-3326-4276-9535-83a622d8e226).
```

## Running via Testcontainer

If you want to run tests against this service, you can add the project to your dependencies 
and use the InteropMockEHRTestContainer class to build a Testcontainer which runs the Docker image.
If you need to do dual-development using Testcontainer with updated changes to MockEHR, _before_ you push 
the changes to the MockEHR Docker Image, you run build a new local image with this command:
```
./gradlew bootJar
docker build --tag=interop-mock-ehr:1.0.0-<VERSION>-SNAPSHOT ./interop-mock-ehr
```
You can then update mock-ehr-testcontainer.yml with the new image tag temporarily, and install the
new version into your other project (which requires Testcontainer).
