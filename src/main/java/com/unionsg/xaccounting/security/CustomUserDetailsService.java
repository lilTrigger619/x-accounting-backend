package com.unionsg.xaccounting.security;

import com.unionsg.xaccounting.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        //var user = userRepository.findById(Long.parseLong(userId))
        var user = userRepository.findById(UUID.fromString((userId)))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(user.getId().toString()) // IMPORTANT for auditing
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().getName())
                .build();
    }
}
