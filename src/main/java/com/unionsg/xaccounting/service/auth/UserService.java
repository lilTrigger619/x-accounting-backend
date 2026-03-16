package com.unionsg.xaccounting.service.auth;

import com.unionsg.xaccounting.dto.auth.CreateUserRequest;
import com.unionsg.xaccounting.dto.auth.RoleResponse;
import com.unionsg.xaccounting.dto.auth.UpdateUserRequest;
import com.unionsg.xaccounting.dto.auth.UserResponse;
import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.UserStatus;
import com.unionsg.xaccounting.repository.PermissionRepository;
import com.unionsg.xaccounting.repository.RoleRepository;
import com.unionsg.xaccounting.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Set<Role> roles = resolveRoles(request.getRoleIds());
        Set<Permission> permissions = resolvePermissions(request.getPermissionIds());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .permissions(permissions)
                .build();

        userRepository.save(user);

        return toResponse(user);
    }

    // ─── READ (single) ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        return toResponse(user);
    }

    // ─── READ (paginated list) ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            user.setStatus(UserStatus.valueOf(request.getStatus()));
        }
        if (request.getRoleIds() != null) {
            user.setRoles(resolveRoles(request.getRoleIds()));
        }
        if (request.getPermissionIds() != null) {
            user.setPermissions(resolvePermissions(request.getPermissionIds()));
        }

        userRepository.save(user);

        return toResponse(user);
    }

    // ─── DELETE (soft — sets status to DISABLED via @SQLDelete) ────────────────

    @Transactional
    public UserResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (user.getStatus() == UserStatus.ACTIVE) {
            userRepository.softDeleteById(id);
            user.setStatus(UserStatus.DISABLED);
        } else {
            userRepository.activateById(id);
            user.setStatus(UserStatus.ACTIVE);
        }

        return toResponse(user);
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptySet();
        Set<Role> roles = roleRepository.findAllById(roleIds)
                .stream().collect(Collectors.toSet());
        if (roles.size() != roleIds.size()) {
            throw new RuntimeException("One or more role IDs not found");
        }
        return roles;
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return Collections.emptySet();
        Set<Permission> permissions = permissionRepository.findAllById(permissionIds)
                .stream().collect(Collectors.toSet());
        if (permissions.size() != permissionIds.size()) {
            throw new RuntimeException("One or more permission IDs not found");
        }
        return permissions;
    }

    private UserResponse toResponse(User user) {

        List<RoleResponse> roles = user.getRoles() == null ? Collections.emptyList()
                : user.getRoles().stream()
                        .map(role -> RoleResponse.builder()
                                .name(role.getName())
                                .permissions(
                                    role.getPermissions() == null ? Collections.emptySet()
                                    : role.getPermissions().stream()
                                            .map(Permission::getName)
                                            .collect(Collectors.toSet())
                                )
                                .build())
                        .collect(Collectors.toList());

        Set<String> directPermissions = user.getPermissions() == null ? Collections.emptySet()
                : user.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus().name())
                .roles(roles)                    // ← pass full RoleResponse list, not just names
                .directPermissions(directPermissions)  // ← match field name in UserResponse
                .build();
    }
}