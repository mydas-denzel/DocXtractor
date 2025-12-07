package com.hng.docxtractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DocXtractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocXtractorApplication.class, args);
    }

}
