package com.renote.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReNoteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReNoteBackendApplication.class, args);
    }
}
