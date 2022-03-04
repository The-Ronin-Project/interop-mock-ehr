# interop-mock-ehr

This is a Spring Boot server which exposes REST APIs for all EHR endpoints
Ronin needs to interact with. Calls to this server should expect identical behavior
to an equivalent EHR counterpart, and data is returned from a MySql database
which Ronin has full control over.

As an example:

HTTP GET on `http://localhost:8080/fhir/Patient/<patient fhir id>` will return an 
R4 Patient resource.


To set up MySql, run this docker code in a terminal:

```
docker run --name=mock-ehr-mysql -p 3306:33060 -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=mock_ehr_db 
-e MYSQL_USER=springuser -e MYSQL_PASSWORD=ThePassword -d mysql/mysql-server:latest-aarch64
```
You might have to get rid of the -aarch64 at the end if you are not using an M1 Mac.

To run the server, use the `./gradlew bootRun` command in a terminal located in the root directory.
