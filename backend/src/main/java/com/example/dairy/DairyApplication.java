package com.example.dairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DairyApplication {
    public static void main(String[] args) {
        SpringApplication.run(DairyApplication.class, args);
    }
}
