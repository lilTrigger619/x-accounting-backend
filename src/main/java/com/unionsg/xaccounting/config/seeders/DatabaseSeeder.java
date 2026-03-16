package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.UserStatus;
import com.unionsg.xaccounting.repository.PermissionRepository;
import com.unionsg.xaccounting.repository.RoleRepository;
import com.unionsg.xaccounting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (permissionRepository.count() > 0) {
            log.info("Database already seeded, skipping...");
            return;
        }

        log.info("Seeding database...");

        // =============================
        // PERMISSION GROUPS
        // =============================

        // User Management
        List<Permission> userManagement = seedPermissionGroup("User Management", List.of(
                "create_user",
                "edit_user",
                "delete_user",
                "view_user",
                "view_users",
                "update_user"
        ));

        // Invoice Management
        List<Permission> invoiceManagement = seedPermissionGroup("Invoice Management", List.of(
                "create_invoice",
                "edit_invoice",
                "delete_invoice",
                "view_invoice"
        ));

        // Role Management
        List<Permission> roleManagement = seedPermissionGroup("Role Management", List.of(
                "create_role",
                "edit_role",
                "delete_role",
                "view_role",
                "assign_role"
        ));

        // Report Management
        List<Permission> reportManagement = seedPermissionGroup("Report Management", List.of(
                "view_report",
                "export_report"
        ));

        // =============================
        // ROLES
        // =============================
        Set<Permission> allPermissions = new HashSet<>();
        allPermissions.addAll(userManagement);
        allPermissions.addAll(invoiceManagement);
        allPermissions.addAll(roleManagement);
        allPermissions.addAll(reportManagement);

        Role superAdmin = createRole("Super Admin", "web", allPermissions);

        Role accountant = createRole("Accountant", "web", new HashSet<>(Set.of(
                findPermission(invoiceManagement, "create_invoice"),
                findPermission(invoiceManagement, "edit_invoice"),
                findPermission(invoiceManagement, "view_invoice"),
                findPermission(reportManagement,  "view_report"),
                findPermission(reportManagement,  "export_report"),
                findPermission(userManagement,    "view_user")
        )));

        Role viewer = createRole("Viewer", "web", new HashSet<>(Set.of(
                findPermission(invoiceManagement, "view_invoice"),
                findPermission(reportManagement,  "view_report"),
                findPermission(userManagement,    "view_user")
        )));

        // =============================
        // USERS
        // =============================
        createUser("admin@xaccounting.com",      "Admin",  "User",  "Admin@1234",      Set.of(superAdmin), Set.of());
        createUser("accountant@xaccounting.com", "John",   "Doe",   "Accountant@1234", Set.of(accountant), Set.of());
        createUser("viewer@xaccounting.com",     "Jane",   "Smith", "Viewer@1234",     Set.of(viewer),     Set.of());

        log.info("Database seeding complete.");
    }

    // =============================
    // HELPERS
    // =============================

    private List<Permission> seedPermissionGroup(String group, List<String> permissionNames) {
        log.info("Seeding permission group: {}", group);
        return permissionNames.stream()
                .map(name -> {
                    Permission p = new Permission();
                    p.setName(name);
                    p.setGuardName(group);   // guardName stores the group name
                    return permissionRepository.save(p);
                })
                .toList();
    }

    private Permission findPermission(List<Permission> group, String name) {
        return group.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Permission not found: " + name));
    }

    private Role createRole(String name, String guardName, Set<Permission> permissions) {
        Role r = new Role();
        r.setName(name);
        r.setGuardName(guardName);
        r.setPermissions(permissions);
        return roleRepository.save(r);
    }

    private void createUser(String email, String firstName, String lastName,
                            String rawPassword, Set<Role> roles, Set<Permission> permissions) {
        User u = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .permissions(permissions)
                .build();
        userRepository.save(u);
    }
}