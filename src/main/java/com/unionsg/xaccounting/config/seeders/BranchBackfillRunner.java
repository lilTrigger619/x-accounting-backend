package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.User.Permission;
import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.branch.BranchAccountingSettings;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.entity.branch.UserAccessScope;
import com.unionsg.xaccounting.entity.branch.UserDimensionDefault;
import com.unionsg.xaccounting.repository.PermissionRepository;
import com.unionsg.xaccounting.repository.RoleRepository;
import com.unionsg.xaccounting.repository.UserRepository;
import com.unionsg.xaccounting.repository.branch.BranchAccountingSettingsRepository;
import com.unionsg.xaccounting.repository.branch.UserAccessScopeRepository;
import com.unionsg.xaccounting.repository.branch.UserDimensionDefaultRepository;
import com.unionsg.xaccounting.service.branch.BranchAccountingSettingsService;
import com.unionsg.xaccounting.service.branch.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Brings an existing install onto branches and cost centers, every start, idempotently (the
 * same steps as manual migration 016):
 * <ol>
 *   <li>widens the journal status CHECK constraint for SUBMITTED / APPROVED / REJECTED;</li>
 *   <li>creates the Head Office branch and its default cost center;</li>
 *   <li>puts every journal and journal line that has no cost center into the Head Office;</li>
 *   <li>once only: gives every existing user Global scope, so nobody loses access;</li>
 *   <li>creates the Viewer / Poster / Approver / Administrator roles if missing, and grants
 *       the new permissions to Super Admin.</li>
 * </ol>
 * Runs after {@code PermissionScanner} (order 5) so the new permissions exist. Each step logs
 * and carries on if it fails, so the application still starts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(6)
public class BranchBackfillRunner implements ApplicationRunner {

    static final List<String> NEW_PERMISSIONS = List.of(
            "manage_branches", "manage_cost_centers", "manage_branch_settings", "manage_user_scopes", "approve_journals");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final BranchService branchService;
    private final BranchAccountingSettingsService settingsService;
    private final BranchAccountingSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final UserAccessScopeRepository scopeRepository;
    private final UserDimensionDefaultRepository defaultRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        step("widen journal status constraint", this::widenStatusConstraint);
        step("backfill Head Office dimensions", () -> tx.executeWithoutResult(s -> backfillDimensions()));
        step("give existing users Global scope", () -> tx.executeWithoutResult(s -> backfillScopes()));
        step("seed roles and permissions", () -> tx.executeWithoutResult(s -> seedRoles()));
    }

    private void step(String name, Runnable body) {
        try {
            body.run();
        } catch (Exception e) {
            log.error("Branch backfill step '{}' failed - skipping so the application can still start. "
                    + "Run db/manual-migrations/016_branches_cost_centers_approvals.sql by hand if it keeps failing.", name, e);
        }
    }

    private void widenStatusConstraint() {
        jdbc.execute("ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_status_check");
        jdbc.execute("ALTER TABLE journal_entries ADD CONSTRAINT journal_entries_status_check CHECK (status IN "
                + "('DRAFT','SUBMITTED','APPROVED','REJECTED','POSTED','REVERSED','CANCELLED'))");
    }

    private void backfillDimensions() {
        CostCenter ho = branchService.ensureHeadOfficeDefaultCostCenter();
        Long branchId = ho.getBranch().getId();
        int lines = jdbc.update("UPDATE journal_lines SET cost_center_id = ?, branch_id = ? WHERE cost_center_id IS NULL",
                ho.getId(), branchId);
        int fixedBranch = jdbc.update("UPDATE journal_lines l SET branch_id = c.branch_id FROM cost_centers c "
                + "WHERE l.cost_center_id = c.id AND (l.branch_id IS NULL OR l.branch_id <> c.branch_id)");
        int journals = jdbc.update("UPDATE journal_entries SET cost_center_id = ?, branch_id = ? WHERE cost_center_id IS NULL",
                ho.getId(), branchId);
        if (lines + journals + fixedBranch > 0) {
            log.info("Backfilled {} journals and {} journal lines into the Head Office ({})", journals, lines, ho.getCode());
        }
    }

    private void backfillScopes() {
        BranchAccountingSettings settings = settingsService.settings();
        if (Boolean.TRUE.equals(settings.getScopeBackfillDone())) {
            return;
        }
        CostCenter ho = branchService.ensureHeadOfficeDefaultCostCenter();
        int n = 0;
        for (User user : userRepository.findAll()) {
            if (!scopeRepository.existsByUserIdAndDeletedFalse(user.getId())) {
                UserAccessScope scope = new UserAccessScope();
                scope.setUser(user);
                scope.setAllCostCenters(true);
                scopeRepository.save(scope);
                n++;
            }
            if (defaultRepository.findForUser(user.getId()).isEmpty()) {
                UserDimensionDefault d = new UserDimensionDefault();
                d.setUser(user);
                d.setCostCenter(ho);
                defaultRepository.save(d);
            }
        }
        settings.setScopeBackfillDone(true);
        settingsRepository.save(settings);
        log.info("Gave {} existing users Global branch scope", n);
    }

    private void seedRoles() {
        List<Permission> all = permissionRepository.findAll();

        roleRepository.findByName("Super Admin").ifPresent(admin -> {
            Set<Permission> perms = admin.getPermissions() == null ? new HashSet<>() : admin.getPermissions();
            boolean changed = false;
            for (Permission p : all) {
                if (NEW_PERMISSIONS.contains(p.getName()) && !perms.contains(p)) {
                    perms.add(p);
                    changed = true;
                }
            }
            if (changed) {
                admin.setPermissions(perms);
                roleRepository.save(admin);
            }
        });

        Predicate<String> view = n -> n.startsWith("view_");
        Predicate<String> post = view.or(n -> n.startsWith("manage_") || n.startsWith("create_") || n.startsWith("post_")
                || n.startsWith("record_") || n.startsWith("allocate_") || n.startsWith("calculate_")
                || n.startsWith("edit_") || n.startsWith("update_"));
        Predicate<String> settingsOnly = n -> n.equals("manage_branches") || n.equals("manage_cost_centers")
                || n.equals("manage_branch_settings") || n.equals("manage_user_scopes") || n.equals("manage_organization")
                || n.equals("manage_accounting_mappings") || n.endsWith("_role") || n.endsWith("_user")
                || n.equals("delete_user") || n.equals("create_user");
        Predicate<String> approve = view.or(n -> n.startsWith("approve_") || n.startsWith("review_"));

        createRole("Viewer", all, view);
        createRole("Poster", all, post.and(settingsOnly.negate()));
        createRole("Approver", all, approve);
        createRole("Administrator", all, n -> true);
    }

    private void createRole(String name, List<Permission> all, Predicate<String> include) {
        if (roleRepository.findByName(name).isPresent()) {
            return;
        }
        Role role = new Role();
        role.setName(name);
        role.setGuardName("web");
        Set<Permission> perms = new HashSet<>();
        for (Permission p : all) {
            if (p.getName() != null && include.test(p.getName())) {
                perms.add(p);
            }
        }
        role.setPermissions(perms);
        roleRepository.save(role);
        log.info("Created the {} role with {} permissions", name, perms.size());
    }
}
