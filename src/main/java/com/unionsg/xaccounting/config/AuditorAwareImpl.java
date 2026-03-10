package com.unionsg.xaccounting.config;


import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.repository.UserRepository;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public class AuditorAwareImpl implements AuditorAware<User> {

    private final UserRepository userRepository;

    public AuditorAwareImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> getCurrentAuditor() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String userId = authentication.getName(); // must store userId as principal

        return userRepository.findById(UUID.fromString(userId));
    }
}
