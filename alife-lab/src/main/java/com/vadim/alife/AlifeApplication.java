package com.vadim.alife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlifeApplication.class, args);
    }
}
