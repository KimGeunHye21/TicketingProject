package com.example.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TicketingProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketingProjectApplication.class, args);
    }

}
