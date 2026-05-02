package com.prathamesh.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class RouteConfig {
    private Map<String,String> routes = new HashMap<>();
}
