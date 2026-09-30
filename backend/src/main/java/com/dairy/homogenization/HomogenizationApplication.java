package com.dairy.homogenization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomogenizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomogenizationApplication.class, args);
    }
}
