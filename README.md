# Distributed version of the Spring PetClinic Sample Application built with Spring Cloud

__InspectIT Demo__

*Based on the original [Spring Petclinic microservices](https://github.com/spring-petclinic/spring-petclinic-microservices) repository.*

## Build the images of the serives

Go to the `petclinic-services` folder and follow the [README.md](./petclinic-services/README.md) file.

## Starting services locally with docker-compose

Once images are ready, you can start them with a single command
`docker-compose up`.

You can execut these configurations:

 * Influxdb + Zipking
 * Prometheus + Jaeger
 * Wavefront + Zipkin

Go to the appropriate repository and execute the command.

After starting services it takes a while for `API Gateway` to be in sync with service registry.

*NOTE: Under MacOSX or Windows, make sure that the Docker VM has enough memory to run the microservices. The default settings
are usually not enough and make the `docker-compose up` painfully slow.*

## References

 * https://github.com/spring-petclinic/spring-petclinic-microservices
