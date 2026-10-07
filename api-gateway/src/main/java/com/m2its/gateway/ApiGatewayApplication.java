package com.m2its.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge service: the only component exposed outside the cluster (via an OpenShift Route).
 * Routes are pure configuration, see application.yml. JWT validation is done by each
 * backend service, so the gateway stays stateless and simple.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
