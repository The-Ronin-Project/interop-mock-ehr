# interop-mock-ehr

This is a Spring Boot server which exposes REST APIs for all EHR endpoints
Ronin needs to interact with. Calls to this server should expect identical behavior
to an equivalent EHR counterpart, and data is returned from a MySql database
which Ronin has full control over.

As an example:

HTTP GET on `http://localhost:8081/fhir/r4/Patient/<patient fhir id>` will return an 
R4 Patient resource.

There will also be a TCP/IP port open for HL7v2 communication at `http://localhost:1011`, or a REST endpoint at 
`http:localhost:8081/HL7overHTTP/`.
Only version 2.5.1 messages are accepted at the moment. If using HTTP, the Content-Type must be set to `application/hl7-v2`.

## Projects

- [interop-mock-ehr](interop-mock-ehr) - to stand up locally with interop-mirth [see here](interop-mock-ehr)

- [interop-mock-ehr-testcontainer](interop-mock-ehr-testcontainer)

Notes below may apply to either project and may need adjusting.

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
The service will be available at port 8081 of your computer. If you're changing code, you may need to delete the generated
build folder from the module project structure before running these commands. Assuming you downloaded the entire repo to run this, the
server will be initialized with the data in the __/init/resources__ directory. Feel free to modify these files locally.

For building M1 images:
```
docker buildx create --name mybuilder
docker buildx use mybuilder
docker buildx build --tag mock-ehr-test -o type=docker --platform=linux/arm64 ./interop-mock-ehr
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

## Swagger/Open API when running locally
The Swagger UI for both the Epic EHR and HAPI r4 APIs with examples for testing is available at 
http://localhost:8081/swagger-ui/index.html. The EHR APIs will be displayed at initial load, use 
the "Select a Definition" dropdown menu at the top of the page to toggle between the different APIs.

Also available:
- The Swagger UI for the HAPI R4 APIs (  __/fhir/r4__ ): [here](http://localhost:8081/fhir/r4/swagger-ui/index.html)
- The same page via an Epic-specific URL ( __/epic/api/fhir/r4__ ): [here](http://localhost:8081/epic/api/FHIR/R4/swagger-ui/index.html)
- The generated Open API YAML: [HAPI R4 here](http://localhost:8081/fhir/r4/api-docs) and [Epic R4 here](http://localhost:8081/epic/api/FHIR/R4/api-docs)
- The Open API JSON for the Epic EHR APIs: [here](http://localhost:8081/v3/api-docs/)

## Test Data

[/init](init) and [/init/resources](/init/resources)
- Test data is available from the Mock EHR on port 8081.
- For test data details see [here](https://github.com/projectronin/interop-mock-ehr/blob/master/init/README.md).
