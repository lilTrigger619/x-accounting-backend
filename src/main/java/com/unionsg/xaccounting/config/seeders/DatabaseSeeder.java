package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.DocumentNumberConfig;
import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.ChartOfAccountClearTo_ENTITY;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.enums.UserStatus;
import com.unionsg.xaccounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;


@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChartOfAccountClearTo_Repository chartOfAccountClearToRepo;
    private final ChartOfAccountRepository chartOfAccountRepo;
    private final DocumentNumberConfigRepository docConfigRepo;
    private List<ChartOfAccountClearTo_ENTITY> assetsClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> liabilityClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> equityClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> revenueClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();
    private List<ChartOfAccountClearTo_ENTITY> expenseClearTo = new ArrayList<ChartOfAccountClearTo_ENTITY>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
//        if (permissionRepository.count() > 0) {
//            log.info("Database already seeded, skipping...");
//            return;
//        }
        if (userRepository.count() == 0){
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
//            log.info("Database already seeded, skipping...");

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
        }

        // ==================================
        // Chart Code clear to
        // ==================================
        if (chartOfAccountClearToRepo.count() == 0){
            // all assets clear to
            assetsClearTo.add( createChartClearToCode(1, "Current Asset"));
            assetsClearTo.add(createChartClearToCode(2, "Fixed Asset"));
            assetsClearTo.add(createChartClearToCode(3, "Other Asset"));
            assetsClearTo.add(createChartClearToCode(4, "Bank"));
            assetsClearTo.add(createChartClearToCode(5, "Cash"));
            assetsClearTo.add(createChartClearToCode(6, "Inventory"));
            assetsClearTo.add(createChartClearToCode(7, "Accounts receivable"));

            // all liabilities clear to codes
            liabilityClearTo.add( createChartClearToCode(8, "Long term liability"));
            liabilityClearTo.add( createChartClearToCode(9, "Current liability"));
            liabilityClearTo.add( createChartClearToCode(10, "Accounts payable"));
            liabilityClearTo.add( createChartClearToCode(11, "Credit card"));
            liabilityClearTo.add( createChartClearToCode(12, "Other liability"));

            // All  Equity clear to codes
            equityClearTo.add(  createChartClearToCode(13, "Owners equity"));
            equityClearTo.add( createChartClearToCode(14, "Retained earnings"));
            equityClearTo.add( createChartClearToCode(15, "Other equity"));

            // All operating revenue codes
            revenueClearTo.add( createChartClearToCode(16, "Operating revenue"));
            revenueClearTo.add( createChartClearToCode(17, "Other income"));
            revenueClearTo.add( createChartClearToCode(18, "Sales"));
            revenueClearTo.add(createChartClearToCode(19, "Sales revenue"));

            // All operating expense codes
            expenseClearTo.add( createChartClearToCode (20, "Direct cost"));
            expenseClearTo.add( createChartClearToCode(21, "Operating expenses"));
            expenseClearTo.add( createChartClearToCode(22, "Payroll expense"));
            expenseClearTo.add( createChartClearToCode(23, "Other expense"));

        }

        // =================================
        // Chart of account
        // ================================

        if (chartOfAccountRepo.count() == 0){
            createChartOfAccounts(1, "Asset", assetsClearTo);
            createChartOfAccounts(2, "Liability", liabilityClearTo);
            createChartOfAccounts(3, "Equity", equityClearTo);
            createChartOfAccounts(4, "Revenue", revenueClearTo);
            createChartOfAccounts(5, "Expense", expenseClearTo);

            log.info("Database seeding complete.");
        }


        // Document config seeding for setting account codes and invoice numbers.
        // we need this to set atleast one invoice number.
        if (docConfigRepo.count() == 0 ){
            DocumentNumberConfig config =  new DocumentNumberConfig();
            config.setModule("INVOICE");
            config.setPrefix("INV");
            config.setPadding(5);
            config.setLastNumber(0L);
            config.setSeparator("-");
            config.setIncludeYear(true);
            config.setResetMonthly(false);
            config.setResetYearly(true);
            config.setCompanyId(1L);
            config.setBranchId(1L);
            config.setCreatedAt(LocalDateTime.now());
            docConfigRepo.save(config);
        }




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

    private ChartOfAccountClearTo_ENTITY createChartClearToCode(long clearToCode, String description){
        ChartOfAccountClearTo_ENTITY ch = ChartOfAccountClearTo_ENTITY.builder()
                .clearToCode(clearToCode)
                .description(description)
                .build();
        return chartOfAccountClearToRepo.save(ch);
    }

    private void createChartOfAccounts(int chartCode, String chartDescription, List<ChartOfAccountClearTo_ENTITY> coa_ct){
       ChartOfAccount coa = ChartOfAccount.builder()
               .coaCode( (long) chartCode)
               .coa_description(chartDescription)
               .coaClearTo(coa_ct)
               .build();
       chartOfAccountRepo.save(coa);
    }
}