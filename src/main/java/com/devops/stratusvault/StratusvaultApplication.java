package com.devops.stratusvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class StratusvaultApplication {

    public static void main(String[] args) {
//        SpringApplication.run(StratusvaultApplication.class, args);
        new SpringApplicationBuilder(StratusvaultApplication.class)
                .profiles("local")
                .run(args);
    }

}
