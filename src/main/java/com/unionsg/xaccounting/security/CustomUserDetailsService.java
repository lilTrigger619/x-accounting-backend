package com.unionsg.xaccounting.security;

import com.unionsg.xaccounting.repository.UserRepository;
import com.unionsg.xaccounting.security.auth.UserPrincipal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;



@Service
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        public CustomUserDetailsService(UserRepository userRepository) {
                this.userRepository = userRepository;
        }

        @Override
        @Transactional
        public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

//        var user = userRepository.findById(Long.parseLong(userId))
//                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

            var user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
            Set<GrantedAuthority> authorities = new HashSet<>();

        // roles
        if (user.getRoles() != null) {
                user.getRoles().forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                // permissions from each role
                if (role.getPermissions() != null) {
                        role.getPermissions().forEach(p ->
                        authorities.add(new SimpleGrantedAuthority(p.getName()))
                        );
                }
                });
        }

        // direct permissions
        if (user.getPermissions() != null) {
                user.getPermissions().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getName()))
                );
        }

        return new UserPrincipal(user, authorities);
        }
}