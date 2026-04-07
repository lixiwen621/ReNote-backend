package com.renote.backend;

import com.renote.backend.config.ForgettingCurveProperties;
import com.renote.backend.config.TaskAttachmentStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ForgettingCurveProperties.class, TaskAttachmentStorageProperties.class})
public class ReNoteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReNoteBackendApplication.class, args);
    }
}
