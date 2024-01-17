package com.antalex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "test")
@Data
public class TestConfig {
    private String prop;
    private String name;
}
