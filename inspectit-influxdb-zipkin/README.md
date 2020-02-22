# Distributed version of the Spring PetClinic Sample Application built with Spring Cloud

__InspectIT Demo__

Example with Influxdb and Zipkin

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

### Metrics

 * Grafana Dashboards - http://localhost:3000

### Trazing

 * Tracing Server (Zipkin) - http://localhost:9411/zipkin/

### Load

 * [Artillery](https://artillery.io/) load generator for the Pet-Clinic. (To ensure that all services are up and running properly.)
