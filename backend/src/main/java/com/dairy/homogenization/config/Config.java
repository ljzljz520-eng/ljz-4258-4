package com.dairy.homogenization.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class Config {
    @Bean
    public ObjectMapper snapshotObjectMapper() {
        return new ObjectMapper();
    }
}
