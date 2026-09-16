package com.unionsg.xaccounting.service.settings;

import com.unionsg.xaccounting.dto.settings.AccountingMappingResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.settings.AccountingMapping;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.enums.settings.SettingType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.settings.AccountingMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The single, central place every automated posting engine (invoice, bill, payment, payroll,
 * closing) asks "which GL account does this business event hit?" (Settings & Setup §8/§46).
 * Rows are self-healing: {@link #resolve} creates a default-valued row the first time a key is
 * looked up if an admin hasn't touched Settings yet, so the app behaves identically to the old
 * hard-coded {@code application.properties} values until someone deliberately retargets one.
 */
@Service
@RequiredArgsConstructor
public class AccountingMappingService {

    private final AccountingMappingRepository repository;
    private final AccountRepository accountRepository;
    private final SettingsAuditLogService auditLogService;

    /** Returns the GL account code currently mapped to this business event. */
    @Transactional
    public String resolve(MappingKey key) {
        return repository.findByMappingKey(key)
                .map(AccountingMapping::getAccountCode)
                .orElseGet(() -> ensureDefault(key).getAccountCode());
    }

    @Transactional
    public List<AccountingMappingResponse> list() {
        return Arrays.stream(MappingKey.values())
                .map(key -> repository.findByMappingKey(key).orElseGet(() -> ensureDefault(key)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountingMappingResponse update(MappingKey key, String newAccountCode, String reason) {
        if (newAccountCode == null || newAccountCode.isBlank()) {
            throw new BusinessException("An account code is required");
        }
        if (!accountRepository.existsByAccountId(newAccountCode)) {
            throw new BusinessException(
                    "Account code '" + newAccountCode + "' does not exist in the Chart of Accounts");
        }

        AccountingMapping mapping = repository.findByMappingKey(key).orElseGet(() -> ensureDefault(key));
        String previousCode = mapping.getAccountCode();
        mapping.setAccountCode(newAccountCode);
        repository.save(mapping);

        if (!newAccountCode.equals(previousCode)) {
            auditLogService.record(SettingType.ACCOUNTING_MAPPING, key.name(), previousCode, newAccountCode, reason);
        }

        return toResponse(mapping);
    }

    private AccountingMapping ensureDefault(MappingKey key) {
        return repository.findByMappingKey(key).orElseGet(() -> {
            AccountingMapping mapping = new AccountingMapping();
            mapping.setMappingKey(key);
            mapping.setAccountCode(key.getDefaultAccountCode());
            return repository.save(mapping);
        });
    }

    private AccountingMappingResponse toResponse(AccountingMapping mapping) {
        MappingKey key = mapping.getMappingKey();
        Optional<AccountEntity> account = accountRepository.findByAccountId(mapping.getAccountCode());
        return AccountingMappingResponse.builder()
                .mappingKey(key.name())
                .group(key.getGroup())
                .label(humanize(key.name()))
                .description(key.getDescription())
                .accountCode(mapping.getAccountCode())
                .accountName(account.map(AccountEntity::getAccountName).orElse(null))
                .configured(account.isPresent())
                .build();
    }

    private String humanize(String enumName) {
        String[] words = enumName.split("_");
        return Arrays.stream(words)
                .map(w -> w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }
}
