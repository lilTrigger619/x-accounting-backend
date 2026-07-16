package com.unionsg.xaccounting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensure a Jackson {@link ObjectMapper} bean exists.
 *
 * Some parts of the codebase directly autowire ObjectMapper (e.g. report audit history metadata normalization).
 *
 * This bean keeps application context initialization stable.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

