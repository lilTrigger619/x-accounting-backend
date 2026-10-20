package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.fixedasset.AssetCategory;
import com.unionsg.xaccounting.entity.fixedasset.FixedAsset;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.fixedasset.AssetCategoryRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetRepository;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Resolves and validates the GL accounts an asset posts to. Each role is resolved the same way:
 * the asset's own account, else its category's, else the {@code MappingKey.FIXED_ASSET_*} mapping.
 * Every resolved account must exist, be active, not be deleted, and have a classification that fits
 * the role (an asset for cost and the contra accounts, an expense for depreciation, impairment and
 * disposal loss, income for disposal gain).
 */
@Component
@RequiredArgsConstructor
public class FixedAssetAccountRules {

    /** Chart group 6 is "Bank Account" and 10 "Accounts Receivable" (see DatabaseSeeder). */
    private static final long CHART_BANK = 6L;
    private static final long CHART_RECEIVABLE = 10L;

    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;
    private final FixedAssetRepository assetRepository;
    private final AssetCategoryRepository categoryRepository;

    public enum Role {
        COST("Fixed asset cost", MappingKey.FIXED_ASSET_COST, AccountType.ASSET),
        ACCUMULATED_DEPRECIATION("Accumulated depreciation", MappingKey.FIXED_ASSET_ACCUMULATED_DEPRECIATION, AccountType.ASSET),
        DEPRECIATION_EXPENSE("Depreciation expense", MappingKey.FIXED_ASSET_DEPRECIATION_EXPENSE, AccountType.EXPENSE),
        ACCUMULATED_IMPAIRMENT("Accumulated impairment", MappingKey.FIXED_ASSET_ACCUMULATED_IMPAIRMENT, AccountType.ASSET),
        IMPAIRMENT_LOSS("Impairment loss", MappingKey.FIXED_ASSET_IMPAIRMENT_LOSS, AccountType.EXPENSE),
        DISPOSAL_GAIN("Gain on disposal", MappingKey.FIXED_ASSET_DISPOSAL_GAIN, AccountType.INCOME),
        DISPOSAL_LOSS("Loss on disposal", MappingKey.FIXED_ASSET_DISPOSAL_LOSS, AccountType.EXPENSE);

        final String label;
        final MappingKey key;
        final AccountType type;

        Role(String label, MappingKey key, AccountType type) {
            this.label = label;
            this.key = key;
            this.type = type;
        }

        public String getLabel() {
            return label;
        }
    }

    /** The account the asset would post to for {@code role} right now. */
    public AccountEntity resolve(FixedAsset asset, Role role) {
        AccountEntity own = assetAccount(asset, role);
        if (own != null) return validate(own, role);
        AccountEntity fromCategory = asset.getCategory() != null ? categoryAccount(asset.getCategory(), role) : null;
        if (fromCategory != null) return validate(fromCategory, role);
        return validate(mapped(role), role);
    }

    /** The category's account for {@code role}, else the mapping's. */
    public AccountEntity resolve(AssetCategory category, Role role) {
        AccountEntity own = categoryAccount(category, role);
        return validate(own != null ? own : mapped(role), role);
    }

    /**
     * Every account the module posts to for each role: those set on assets and categories plus the
     * mapping's. Statements use this to recognise fixed asset accounts and the module's P&L lines.
     */
    public Map<Role, Set<Long>> configuredAccountIds() {
        Map<Role, Set<Long>> out = new EnumMap<>(Role.class);
        for (Role role : Role.values()) out.put(role, new HashSet<>());
        for (FixedAsset a : assetRepository.findByDeletedFalseOrderByAssetNumberAsc()) {
            for (Role role : Role.values()) add(out.get(role), assetAccount(a, role));
        }
        for (AssetCategory c : categoryRepository.findByDeletedFalseOrderByNameAsc()) {
            for (Role role : Role.values()) add(out.get(role), categoryAccount(c, role));
        }
        for (Role role : Role.values()) {
            try {
                accountRepository.findByAccountId(accountingMappingService.resolve(role.key)).ifPresent(a -> add(out.get(role), a));
            } catch (RuntimeException ignored) {
                // An unmapped role simply contributes no account.
            }
        }
        return out;
    }

    private static void add(Set<Long> ids, AccountEntity a) {
        if (a != null) ids.add(a.getId());
    }

    /** Checks an account picked for {@code role} and returns it. */
    public AccountEntity validate(AccountEntity account, Role role) {
        assertUsable(account, role.label);
        AccountType type = typeOf(account);
        if (type != role.type) {
            throw new BusinessException(role.label + " account " + describe(account) + " is " + label(type)
                    + "; it must be " + label(role.type) + ".");
        }
        if (role.type == AccountType.ASSET) {
            Long chart = chartCode(account);
            if (chart != null && (chart == CHART_BANK || chart == CHART_RECEIVABLE)) {
                throw new BusinessException(role.label + " account " + describe(account)
                        + " is a bank or receivable account and can't hold fixed assets.");
            }
            if (Boolean.TRUE.equals(account.getIsControlAccount())) {
                throw new BusinessException(role.label + " account " + describe(account)
                        + " is a control account owned by another module.");
            }
        }
        return account;
    }

    /** The account an OFFSET_ACCOUNT acquisition credits: active and not owned by another module. */
    public AccountEntity validateOffset(AccountEntity account) {
        assertUsable(account, "Offset");
        if (Boolean.TRUE.equals(account.getIsControlAccount())) {
            throw new BusinessException("Offset account " + describe(account)
                    + " is a control account owned by another module. Pick another account.");
        }
        return account;
    }

    /** Proceeds land on an asset account (cash, bank or a receivable that is not a control account). */
    public AccountEntity validateProceeds(AccountEntity account) {
        assertUsable(account, "Proceeds");
        if (typeOf(account) != AccountType.ASSET) {
            throw new BusinessException("Proceeds account " + describe(account) + " must be an asset account.");
        }
        if (Boolean.TRUE.equals(account.getIsControlAccount())) {
            throw new BusinessException("Proceeds account " + describe(account) + " is a control account owned by another module.");
        }
        return account;
    }

    /** A cost and its contra accounts must be three different accounts. */
    public void assertDistinct(AccountEntity cost, AccountEntity accumulatedDepreciation, AccountEntity accumulatedImpairment) {
        if (cost.getId().equals(accumulatedDepreciation.getId())) {
            throw new BusinessException("The cost and accumulated depreciation accounts must be different accounts.");
        }
        if (cost.getId().equals(accumulatedImpairment.getId())) {
            throw new BusinessException("The cost and accumulated impairment accounts must be different accounts.");
        }
    }

    public AccountEntity byId(Long id, String what) {
        if (id == null) return null;
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(what + " account not found (id " + id + ")"));
    }

    public AccountEntity byCode(String code, String what) {
        return accountRepository.findByAccountId(code)
                .orElseThrow(() -> new BusinessException(what + " account " + code + " does not exist in the Chart of Accounts."));
    }

    public static AccountType typeOf(AccountEntity account) {
        if (account.getCoaClearTo() == null || account.getCoaClearTo().getChartOfAccount() == null) {
            return null;
        }
        return account.getCoaClearTo().getChartOfAccount().getAccountType();
    }

    public static String describe(AccountEntity a) {
        return a.getAccountId() + " " + a.getAccountName();
    }

    private AccountEntity mapped(Role role) {
        String code = accountingMappingService.resolve(role.key);
        return accountRepository.findByAccountId(code)
                .orElseThrow(() -> new BusinessException("Required accounting configuration missing: \""
                        + role.key.getDescription() + "\" is mapped to account code \"" + code
                        + "\", which does not exist in the Chart of Accounts. Configure it under Settings > Accounting Mappings."));
    }

    private static void assertUsable(AccountEntity account, String what) {
        if (account == null) {
            throw new BusinessException(what + " account is required.");
        }
        if (account.isDeleted()) {
            throw new BusinessException(what + " account " + describe(account) + " has been deleted.");
        }
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new BusinessException(what + " account " + describe(account) + " is inactive.");
        }
    }

    private static Long chartCode(AccountEntity account) {
        return account.getCoaClearTo() != null && account.getCoaClearTo().getChartOfAccount() != null
                ? account.getCoaClearTo().getChartOfAccount().getCoaCode() : null;
    }

    private static String label(AccountType type) {
        if (type == null) return "unclassified";
        return switch (type) {
            case ASSET -> "an asset account";
            case LIABILITY -> "a liability account";
            case EQUITY -> "an equity account";
            case INCOME -> "an income account";
            case EXPENSE -> "an expense account";
        };
    }

    public static AccountEntity assetAccount(FixedAsset a, Role role) {
        return pick(role, a, FixedAsset::getCostAccount, FixedAsset::getAccumulatedDepreciationAccount,
                FixedAsset::getDepreciationExpenseAccount, FixedAsset::getAccumulatedImpairmentAccount,
                FixedAsset::getImpairmentLossAccount, FixedAsset::getDisposalGainAccount, FixedAsset::getDisposalLossAccount);
    }

    public static AccountEntity categoryAccount(AssetCategory c, Role role) {
        return pick(role, c, AssetCategory::getCostAccount, AssetCategory::getAccumulatedDepreciationAccount,
                AssetCategory::getDepreciationExpenseAccount, AssetCategory::getAccumulatedImpairmentAccount,
                AssetCategory::getImpairmentLossAccount, AssetCategory::getDisposalGainAccount, AssetCategory::getDisposalLossAccount);
    }

    @SafeVarargs
    private static <T> AccountEntity pick(Role role, T owner, Function<T, AccountEntity>... getters) {
        return getters[role.ordinal()].apply(owner);
    }
}
