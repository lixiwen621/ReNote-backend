package com.renote.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.renote.backend.mapper")
public class ReNoteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReNoteBackendApplication.class, args);
    }
}
