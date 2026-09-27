package com.openinsure.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ClaimWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimWorkerApplication.class, args);
    }
}
