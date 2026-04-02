package com.pickme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class PickMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickMeApplication.class, args);
    }
}
