package com.openinsure.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableKafka
@SpringBootApplication
public class NotificationServiceApplication {
    @Bean
    ObjectMapper notificationObjectMapper() {
        return new ObjectMapper();
    }

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
