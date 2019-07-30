# Distributed version of the Spring PetClinic Sample Application built with Spring Cloud

__OpenAPM Demo:__ OpenCensus, Jaeger, Prometheus, Grafana

## Starting services locally without Docker

Every microservice is a Spring Boot application and can be started locally using IDE or `../mvnw spring-boot:run` command.

Please note that supporting services (__Config and Discovery Server__) must be started before any other application (Customers, Vets, Visits and API).

Startup of __Tracing server__, __Admin server__, __Grafana__ and __Prometheus__ is optional.

If everything goes well, you can access the following services at given location:

### Services

  * AngularJS frontend (API Gateway) - http://localhost:8080
  * Customers, Vets and Visits Services - random port, check Eureka Dashboard

### Infrastructure

 * Discovery Server - http://localhost:8761
 * Config Server - http://localhost:8888
 * Admin Server (Spring Boot Admin) - http://localhost:9090

### Monitoring

  * Grafana Dashboards - http://localhost:3000
  * Prometheus - http://localhost:9091
  * Hystrix Dashboard for Circuit Breaker pattern - http://localhost:7979 - On the home page is a form where you can enter
  the URL for an event stream to monitor, for example the `api-gateway` service running locally: `http://localhost:8080/actuator/hystrix.stream`
  or running into docker: `http://api-gateway:8080/actuator/hystrix.stream`

### Trazing

 * Tracing Server (Zipkin) - http://localhost:9411/zipkin/ (we use [openzipkin](https://github.com/openzipkin/zipkin/tree/master/zipkin-server))

### Load

 * Gatling load generator for the Pet-Clinic. Load generation starts with a delay of 5 minutes! (To ensure that all services are up and running properly.)

## Starting services locally with docker-compose

In order to start entire infrastructure using Docker, you have to build images by executing `./mvnw clean install -PbuildDocker`
from a project root.

Once images are ready, you can start them with a single command
`docker-compose up`.

After starting services it takes a while for `API Gateway` to be in sync with service registry, so don't be scared of initial Zuul timeouts.


## Database configuration

In its default configuration, Petclinic uses an in-memory database (HSQLDB) which gets populated at startup with data.

A similar setup is provided for MySql in case a persistent database configuration is needed.

Dependency for Connector/J, the MySQL JDBC driver is already included in the `pom.xml` files.

### Start a MySql database

You may start a MySql database with docker:

```
docker run -e MYSQL_ROOT_PASSWORD=petclinic -e MYSQL_DATABASE=petclinic -p 3306:3306 mysql:5.7.8
```

or download and install the MySQL database (e.g., MySQL Community Server 5.7 GA), which can be found here: https://dev.mysql.com/downloads/

### Use the Spring 'mysql' profile

To use a MySQL database, you have to start 3 microservices (`visits-service`, `customers-service` and `vets-services`)
with the `mysql` Spring profile. Add the `--spring.profiles.active=mysql` as programm argument.

By default, at startup, database schema will be created and data will be populated.
You may also manualy create the PetClinic database and data by executing the `"db/mysql/{schema,data}.sql"` scripts of each 3 microservices.
In the `application.yml` of the [Configuration repository], set the `initialization-mode` to `never`.

If you are running the microservices with Docker, you have to add the `mysql` profile into the (Dockerfile)[docker/Dockerfile]:
```
ENV SPRING_PROFILES_ACTIVE docker,mysql
```
In the `mysql section` of the `application.yml` from the [Configuration repository], you have to change
the host and port of your MySQL JDBC connection string.
