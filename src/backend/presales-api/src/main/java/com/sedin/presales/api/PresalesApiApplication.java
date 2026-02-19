package com.sedin.presales.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sedin.presales")
@EntityScan(basePackages = "com.sedin.presales.domain.entity")
@EnableJpaRepositories(basePackages = "com.sedin.presales.infrastructure.persistence")
public class PresalesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PresalesApiApplication.class, args);
    }
}
