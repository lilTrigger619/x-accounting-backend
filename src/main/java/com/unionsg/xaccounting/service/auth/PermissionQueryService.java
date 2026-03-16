package com.unionsg.xaccounting.service.auth;

import com.unionsg.xaccounting.dto.auth.permission.PermissionResponse;
import com.unionsg.xaccounting.enums.PermissionStatus;
import com.unionsg.xaccounting.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unionsg.xaccounting.entity.User.Permission;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionQueryService {

    private final PermissionRepository permissionRepository;

    public PermissionQueryService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .guardName(p.getGuardName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public PermissionResponse togglePermissionStatus(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found with id: " + id));

        if (permission.getStatus() == PermissionStatus.ACTIVE) {
            permission.setStatus(PermissionStatus.DISABLED);
        } else {
            permission.setStatus(PermissionStatus.ACTIVE);
        }

        permissionRepository.save(permission);
        return toResponse(permission);
    }

    private PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .guardName(permission.getGuardName())
                .status(permission.getStatus().name())
                .build();
    }
}