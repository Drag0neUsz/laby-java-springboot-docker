package com.example.SpringBootApp.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Entry point for the "Thick Client" console application.
 *
 * Run as a NON-web Spring Boot app so we don't accidentally bind a port -
 * everything happens via the interactive console (CommandLineRunner) and a
 * single RestTemplate that talks to the three microservices over HTTP.
 */
@SpringBootApplication
public class ThickClientApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ThickClientApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
