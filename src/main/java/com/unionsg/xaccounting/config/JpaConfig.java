package com.unionsg.xaccounting.config;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.security.auth.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    @Bean
    public AuditorAware<User> auditorAware() {
        return () -> {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof UserPrincipal userPrincipal) {
                return Optional.of(userPrincipal.getUser());
            }

            return Optional.empty();
        };


    }
}