package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.dto.branch.BranchAccountingSettingsRequest;
import com.unionsg.xaccounting.dto.branch.BranchAccountingSettingsResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.branch.BranchAccountingSettings;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.branch.BranchAccountingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BranchAccountingSettingsService {

    private final BranchAccountingSettingsRepository repository;
    private final AccountRepository accountRepository;

    /** The settings row, created with the defaults (self-balancing on, approvals on) if missing. */
    @Transactional
    public BranchAccountingSettings settings() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> repository.save(new BranchAccountingSettings()));
    }

    @Transactional
    public BranchAccountingSettingsResponse get() {
        return toResponse(settings());
    }

    @Transactional
    public BranchAccountingSettingsResponse update(BranchAccountingSettingsRequest request) {
        BranchAccountingSettings s = settings();
        if (request.getSelfBalancingBranches() != null) {
            s.setSelfBalancingBranches(request.getSelfBalancingBranches());
        }
        if (request.getJournalApprovalRequired() != null) {
            s.setJournalApprovalRequired(request.getJournalApprovalRequired());
        }
        BigDecimal threshold = request.getApprovalThreshold();
        if (threshold != null && threshold.signum() < 0) {
            throw new BusinessException("The approval threshold can't be negative");
        }
        s.setApprovalThreshold(threshold);
        if (request.getInterBranchAccountId() == null) {
            s.setInterBranchAccount(null);
        } else {
            AccountEntity account = accountRepository.findById(request.getInterBranchAccountId())
                    .orElseThrow(() -> new BusinessException("Account not found with ID: " + request.getInterBranchAccountId()));
            com.unionsg.xaccounting.enums.AccountType type = account.getCoaClearTo() != null
                    && account.getCoaClearTo().getChartOfAccount() != null
                    ? account.getCoaClearTo().getChartOfAccount().getAccountType() : null;
            if (type != com.unionsg.xaccounting.enums.AccountType.ASSET
                    && type != com.unionsg.xaccounting.enums.AccountType.LIABILITY) {
                throw new BusinessException("The inter-branch account must be an asset or liability account, so it shows on each branch's balance sheet");
            }
            s.setInterBranchAccount(account);
        }
        return toResponse(repository.save(s));
    }

    private static BranchAccountingSettingsResponse toResponse(BranchAccountingSettings s) {
        AccountEntity account = s.getInterBranchAccount();
        return BranchAccountingSettingsResponse.builder()
                .selfBalancingBranches(Boolean.TRUE.equals(s.getSelfBalancingBranches()))
                .interBranchAccountId(account != null ? account.getId() : null)
                .interBranchAccountCode(account != null ? account.getAccountId() : null)
                .interBranchAccountName(account != null ? account.getAccountName() : null)
                .journalApprovalRequired(Boolean.TRUE.equals(s.getJournalApprovalRequired()))
                .approvalThreshold(s.getApprovalThreshold())
                .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt())
                .build();
    }
}
