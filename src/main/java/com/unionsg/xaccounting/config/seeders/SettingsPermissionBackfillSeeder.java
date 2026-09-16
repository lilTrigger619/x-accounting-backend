package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.repository.PermissionRepository;
import com.unionsg.xaccounting.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Existing installs seeded their "Super Admin" role once, with whatever permissions existed at
 * that time (see {@code DatabaseSeeder}). New permissions {@link PermissionScanner} discovers
 * afterwards - like this module's own "Settings" group - are never retroactively granted to any
 * role; an admin has to attach them by hand from the Roles screen. Without this, the account
 * that seeded the database would be locked out of the very Settings screens that let it grant
 * itself access, on every existing install. Runs after {@link PermissionScanner} (§25/§26 -
 * every role's permissions stay explicit and auditable; this just keeps the already-privileged
 * seed admin privileged after a feature upgrade, exactly as if an admin had ticked the new boxes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(6)
public class SettingsPermissionBackfillSeeder implements ApplicationRunner {

    private static final String SUPER_ADMIN_ROLE = "Super Admin";
    private static final List<String> SETTINGS_PERMISSIONS = List.of(
            "view_settings", "manage_accounting_mappings", "manage_organization", "manage_bank_accounts");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role superAdmin = roleRepository.findByName(SUPER_ADMIN_ROLE).orElse(null);
        if (superAdmin == null) {
            return;
        }

        boolean changed = false;
        for (String name : SETTINGS_PERMISSIONS) {
            Permission permission = permissionRepository.findByName(name).orElse(null);
            if (permission != null && !superAdmin.getPermissions().contains(permission)) {
                superAdmin.getPermissions().add(permission);
                changed = true;
            }
        }

        if (changed) {
            roleRepository.save(superAdmin);
            log.info("Granted the Settings permission group to the {} role", SUPER_ADMIN_ROLE);
        }
    }
}
