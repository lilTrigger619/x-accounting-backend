package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.bankrec.BankMatchingRule;
import com.unionsg.xaccounting.entity.bankrec.BankStatementImportProfile;
import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;
import com.unionsg.xaccounting.repository.PermissionRepository;
import com.unionsg.xaccounting.repository.RoleRepository;
import com.unionsg.xaccounting.repository.bankrec.BankMatchingRuleRepository;
import com.unionsg.xaccounting.repository.bankrec.BankStatementImportProfileRepository;
import com.unionsg.xaccounting.service.bankrec.BankReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bank Reconciliation setup, idempotent on every start: grants the module's permissions to the
 * Super Admin role (same reasoning as {@link SettingsPermissionBackfillSeeder}), creates the
 * override permission (checked in the service, so the scanner never sees it), and seeds one
 * standard matching rule and one generic CSV mapping so the module works out of the box. Both
 * seeded rows are ordinary setup records users can edit or delete.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(7)
public class BankReconciliationSetupSeeder implements ApplicationRunner {

    private static final String SUPER_ADMIN_ROLE = "Super Admin";
    private static final String GROUP = "Bank Reconciliation";
    private static final List<String> PERMISSIONS = List.of(
            "view_bank_reconciliation", "create_bank_reconciliation", "delete_bank_reconciliation",
            "post_reconciliation_adjustment", "review_bank_reconciliation", "complete_bank_reconciliation",
            "reopen_bank_reconciliation", "manage_bank_reconciliation_setup",
            BankReconciliationService.OVERRIDE_PERMISSION);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final BankMatchingRuleRepository ruleRepository;
    private final BankStatementImportProfileRepository profileRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!permissionRepository.existsByNameAndGuardName(BankReconciliationService.OVERRIDE_PERMISSION, GROUP)
                && permissionRepository.findByName(BankReconciliationService.OVERRIDE_PERMISSION).isEmpty()) {
            Permission permission = new Permission();
            permission.setName(BankReconciliationService.OVERRIDE_PERMISSION);
            permission.setGuardName(GROUP);
            permissionRepository.save(permission);
        }

        Role superAdmin = roleRepository.findByName(SUPER_ADMIN_ROLE).orElse(null);
        if (superAdmin != null) {
            boolean changed = false;
            for (String name : PERMISSIONS) {
                Permission permission = permissionRepository.findByName(name).orElse(null);
                if (permission != null && !superAdmin.getPermissions().contains(permission)) {
                    superAdmin.getPermissions().add(permission);
                    changed = true;
                }
            }
            if (changed) {
                roleRepository.save(superAdmin);
                log.info("Granted the Bank Reconciliation permissions to the {} role", SUPER_ADMIN_ROLE);
            }
        }

        if (!ruleRepository.existsByDeletedFalse()) {
            BankMatchingRule rule = new BankMatchingRule();
            rule.setName("Standard match");
            rule.setDescription("Exact amount within 3 days. Matched straight away at 85+, suggested at 60+.");
            rule.setPriority(100);
            ruleRepository.save(rule);
        }

        if (!profileRepository.existsByDeletedFalse()) {
            BankStatementImportProfile profile = new BankStatementImportProfile();
            profile.setName("Generic CSV (Date, Description, Reference, Debit, Credit, Balance)");
            profile.setDescription("Header row with separate Debit and Credit columns. Copy and adjust for your bank.");
            profile.setTransactionDateColumn("Date");
            profile.setValueDateColumn("Value Date");
            profile.setDescriptionColumn("Description");
            profile.setReferenceColumn("Reference");
            profile.setDebitColumn("Debit");
            profile.setCreditColumn("Credit");
            profile.setBalanceColumn("Balance");
            profile.setAmountSignConvention(AmountSignConvention.POSITIVE_IS_CREDIT);
            profileRepository.save(profile);
        }
    }
}
