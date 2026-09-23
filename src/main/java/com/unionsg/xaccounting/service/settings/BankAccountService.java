package com.unionsg.xaccounting.service.settings;

import com.unionsg.xaccounting.dto.settings.BankAccountResponse;
import com.unionsg.xaccounting.dto.settings.SaveBankAccountRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.enums.settings.SettingType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository repository;
    private final AccountRepository accountRepository;
    private final SettingsAuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<BankAccountResponse> list() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public BankAccountResponse create(SaveBankAccountRequest request) {
        validateGlAccount(request.getGlAccountCode());
        BankAccount account = new BankAccount();
        applyRequest(account, request);
        BankAccount saved = repository.save(account);
        auditLogService.record(SettingType.BANK_ACCOUNT, saved.getId().toString(),
                null, describe(saved), "Bank account created");
        return toResponse(saved);
    }

    @Transactional
    public BankAccountResponse update(Long id, SaveBankAccountRequest request) {
        validateGlAccount(request.getGlAccountCode());
        BankAccount account = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Bank account not found: " + id));
        String previous = describe(account);
        applyRequest(account, request);
        BankAccount saved = repository.save(account);
        String updated = describe(saved);
        if (!updated.equals(previous)) {
            auditLogService.record(SettingType.BANK_ACCOUNT, id.toString(), previous, updated, null);
        }
        return toResponse(saved);
    }

    @Transactional
    public BankAccountResponse toggleStatus(Long id) {
        BankAccount account = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Bank account not found: " + id));
        BankAccountStatus previousStatus = account.getStatus();
        account.setStatus(previousStatus == BankAccountStatus.ACTIVE
                ? BankAccountStatus.INACTIVE : BankAccountStatus.ACTIVE);
        BankAccount saved = repository.save(account);
        auditLogService.record(SettingType.BANK_ACCOUNT, id.toString(),
                previousStatus.name(), saved.getStatus().name(), null);
        return toResponse(saved);
    }

    private String describe(BankAccount account) {
        return account.getAccountName() + " (" + account.getGlAccountCode() + ")";
    }

    private void validateGlAccount(String code) {
        if (code == null || !accountRepository.existsByAccountId(code)) {
            throw new BusinessException("GL account code '" + code + "' does not exist in the Chart of Accounts");
        }
    }

    private void applyRequest(BankAccount account, SaveBankAccountRequest request) {
        account.setBankName(request.getBankName());
        account.setAccountName(request.getAccountName());
        account.setAccountNumber(request.getAccountNumber());
        account.setCurrency(request.getCurrency());
        account.setGlAccountCode(request.getGlAccountCode());
        account.setBranch(request.getBranch());
        if (request.getIsDefault() != null) {
            account.setIsDefault(request.getIsDefault());
        }
        if (request.getEnableReconciliation() != null) {
            account.setEnableReconciliation(request.getEnableReconciliation());
        }
    }

    private BankAccountResponse toResponse(BankAccount account) {
        Optional<AccountEntity> glAccount = accountRepository.findByAccountId(account.getGlAccountCode());
        return BankAccountResponse.builder()
                .id(account.getId())
                .bankName(account.getBankName())
                .accountName(account.getAccountName())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .glAccountCode(account.getGlAccountCode())
                .glAccountName(glAccount.map(AccountEntity::getAccountName).orElse(null))
                .branch(account.getBranch())
                .status(account.getStatus())
                .isDefault(account.getIsDefault())
                .enableReconciliation(account.getEnableReconciliation())
                .build();
    }
}
