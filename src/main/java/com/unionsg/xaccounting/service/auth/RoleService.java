package com.unionsg.xaccounting.service.auth;

import com.unionsg.xaccounting.dto.auth.role.CreateRoleRequest;
import com.unionsg.xaccounting.dto.auth.role.RoleDetailResponse;
import com.unionsg.xaccounting.dto.auth.role.UpdateRoleRequest;
import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.repository.PermissionRepository;
import com.unionsg.xaccounting.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unionsg.xaccounting.enums.RoleStatus;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    // ─── GET ALL ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RoleDetailResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RoleDetailResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        return toResponse(role);
    }

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @Transactional
    public RoleDetailResponse createRole(CreateRoleRequest request) {

        if (roleRepository.existsByName(request.getName())) {
            throw new RuntimeException("Role with name '" + request.getName() + "' already exists");
        }

        Set<Permission> permissions = resolvePermissions(request.getPermissionIds());

        Role role = Role.builder()
                .name(request.getName())
                .guardName(request.getGuardName() != null ? request.getGuardName() : "web")
                .permissions(permissions)
                .build();

        roleRepository.save(role);

        return toResponse(role);
    }

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    @Transactional
    public RoleDetailResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByName(request.getName())) {
                throw new RuntimeException("Role with name '" + request.getName() + "' already exists");
            }
            role.setName(request.getName());
        }

        if (request.getPermissionIds() != null) {
            role.setPermissions(resolvePermissions(request.getPermissionIds()));
        }

        roleRepository.save(role);

        return toResponse(role);
    }

    // ─── DELETE (hard delete — removes role and its assignments) ───────────────

    @Transactional
    public RoleDetailResponse toggleRoleStatus(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

        if (role.getStatus() == RoleStatus.ACTIVE) {
            role.setStatus(RoleStatus.DISABLED);
        } else {
            role.setStatus(RoleStatus.ACTIVE);
        }

        roleRepository.save(role);
        return toResponse(role);
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return Collections.emptySet();
        Set<Permission> permissions = permissionRepository.findAllById(permissionIds)
                .stream().collect(Collectors.toSet());
        if (permissions.size() != permissionIds.size()) {
            throw new RuntimeException("One or more permission IDs not found");
        }
        return permissions;
    }

    private RoleDetailResponse toResponse(Role role) {
        Set<String> permissionNames = role.getPermissions() == null ? Collections.emptySet()
                : role.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet());

        return RoleDetailResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .guardName(role.getGuardName())
                .status(role.getStatus().name())
                .permissions(permissionNames)
                .build();
    }
}