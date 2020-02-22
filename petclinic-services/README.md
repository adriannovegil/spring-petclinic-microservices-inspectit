# Distributed version of the Spring PetClinic Sample Application built with Spring Cloud

__InspectIT Demo__

*Based on the original [Spring Petclinic microservices](https://github.com/spring-petclinic/spring-petclinic-microservices) repository.*

## Starting services locally without Docker

Every microservice is a Spring Boot application and can be started locally using IDE or `./mvnw spring-boot:run` command.

Please note that supporting services (__Config and Discovery Server__) must be started before any other application (Customers, Vets, Visits and API).

Startup of __Tracing server__, __Admin server__, __Grafana__ and __Prometheus__ is optional.

## Build service images

In order to start entire infrastructure using Docker, you have to build images by executing `./mvnw clean install -PbuildDocker`
from a project root.

After starting services it takes a while for `API Gateway` to be in sync with service registry, so don't be scared of initial Zuul timeouts.

*NOTE: Under MacOSX or Windows, make sure that the Docker VM has enough memory to run the microservices. The default settings
are usually not enough and make the `docker-compose up` painfully slow.*

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

## Services

If everything goes well, you can access the following services at given location:

### Business services

  * AngularJS frontend (API Gateway) - http://localhost:8080
  * Customers, Vets and Visits Services - random port, check Eureka Dashboard

### Infrastructure

 * Discovery Server - http://localhost:8761
 * Config Server - http://localhost:8888
 * Admin Server (Spring Boot Admin) - http://localhost:9090

### Microservices management

  * Hystrix Dashboard for Circuit Breaker pattern - http://localhost:7979 - On the home page is a form where you can enter
 the URL for an event stream to monitor, for example the `api-gateway` service running locally: `http://localhost:8080/actuator/hystrix.stream`
 or running into docker: `http://api-gateway:8080/actuator/hystrix.stream`

### Load

 * [Artillery](https://artillery.io/) load generator for the Pet-Clinic. (To ensure that all services are up and running properly.)
