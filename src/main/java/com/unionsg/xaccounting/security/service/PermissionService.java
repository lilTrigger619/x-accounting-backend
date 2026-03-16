package com.unionsg.xaccounting.security.service;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import org.springframework.stereotype.Service;

import com.unionsg.xaccounting.enums.PermissionStatus;
import com.unionsg.xaccounting.enums.RoleStatus;

@Service
public class PermissionService {

    public boolean currentUserHasPermission(String permissionName) {

        User user = SecurityUtils.getCurrentUser();

        if (user == null) return false;

        // Direct permissions — only ACTIVE ones
        boolean directPermission = user.getPermissions()
                .stream()
                .filter(p -> p.getStatus() == PermissionStatus.ACTIVE) // ← skip disabled
                .anyMatch(p -> p.getName().equals(permissionName));

        if (directPermission) return true;

        // Role permissions — only from ACTIVE roles with ACTIVE permissions
        return user.getRoles()
                .stream()
                .filter(role -> role.getStatus() == RoleStatus.ACTIVE)
                .flatMap(role -> role.getPermissions().stream())
                .filter(p -> p.getStatus() == PermissionStatus.ACTIVE) // ← skip disabled
                .anyMatch(p -> p.getName().equals(permissionName));
    }
}