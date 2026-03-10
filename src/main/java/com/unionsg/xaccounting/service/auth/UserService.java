package com.unionsg.xaccounting.service.auth;

import com.unionsg.xaccounting.enums.UserStatus;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.dto.auth.CreateUserRequest;
import com.unionsg.xaccounting.dto.auth.UserResponse;
import com.unionsg.xaccounting.repository.RoleRepository;
import com.unionsg.xaccounting.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.ACTIVE)
                .role(role)
                .build();

        userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(role.getName())
                .status(user.getStatus().name())
                .build();
    }
}
