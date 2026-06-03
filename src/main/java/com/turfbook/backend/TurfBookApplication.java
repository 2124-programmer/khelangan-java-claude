package com.turfbook.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TurfBookApplication {

    public static void main(String[] args) {
        
        SpringApplication.run(TurfBookApplication.class, args);
        System.out.println("Welcome to TurfBook backend is running successfully!");
    }
}
