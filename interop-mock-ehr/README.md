# interop-mock-ehr

### Running Locally with Mirth

From the top level folder, build the `interop-mock-ehr` repo then
run docker build on the `interop-mock-ehr` project.
```
./gradlew clean build --refresh-dependencies
docker build --tag mock-ehr-test:1.2.2-INT2038-SNAPSHOT ./interop-mock-ehr
```

Your session should end like this:
```
.
.
.
=> => writing image sha256:1fa22735f8f607b99f0f1489fb65532be0f012b6a0df8b3855bfb1215f79cbb9                                                                                                                                                                                                      
=> => naming to docker.io/library/mock-ehr-test:1.2.2-INT2038-SNAPSHOT    
```

Now in interop-mirth, edit this file: 

`mirth-channel-config/dev-env/docker-compose.yaml`

Modify the `image:` setting under `mockehr:` to match your docker build name:
```
mockehr:
  restart: on-failure
    # image: docker-proxy.devops.projectronin.io/interop-mock-ehr:latest
    image: docker.io/library/mock-ehr-test:1.2.2-INT2038-SNAPSHOT
    container_name: mirth-mock-ehr
```

While in IntelliJ editing the `docker-compose.yaml` file, do one of the following:
- (If prompted by a Gradle icon in IntelliJ) click it to Load Gradle Changes.
- (Or if not prompted) in the Gradle tab select Reload Gradle Projects.

Now when you `./gradlew mirth` it will use your local mock EHR image.
