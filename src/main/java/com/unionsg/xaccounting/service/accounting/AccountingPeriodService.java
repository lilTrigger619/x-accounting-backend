package com.unionsg.xaccounting.service.accounting;

import com.unionsg.xaccounting.dto.accounting.AccountingPeriodResponse;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.enums.FinancialPeriodAction;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountingPeriodService {

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final FinancialPeriodAuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AccountingPeriodResponse> getByFinancialYear(Long financialYearId) {
        return accountingPeriodRepository.findByFinancialYearIdOrderByPeriodNumberAsc(financialYearId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountingPeriod getEntity(Long id) {
        return accountingPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting Period not found"));
    }

    @Transactional
    public AccountingPeriodResponse lock(Long id, String reason) {
        AccountingPeriod period = getEntity(id);
        if (period.getStatus() != AccountingPeriodStatus.OPEN) {
            throw new BadRequestException("Only an open period can be locked");
        }
        String previous = period.getStatus().name();
        period.setStatus(AccountingPeriodStatus.LOCKED);
        period.setLockedAt(LocalDateTime.now());
        period.setLockedBy(SecurityUtils.getCurrentUser());
        AccountingPeriod saved = accountingPeriodRepository.save(period);

        auditLogService.record(FinancialPeriodEntityType.ACCOUNTING_PERIOD, saved.getId(),
                FinancialPeriodAction.LOCKED, previous, saved.getStatus().name(), reason);

        return toResponse(saved);
    }

    @Transactional
    public AccountingPeriodResponse unlock(Long id, String reason) {
        AccountingPeriod period = getEntity(id);
        if (period.getStatus() != AccountingPeriodStatus.LOCKED) {
            throw new BadRequestException("Only a locked period can be unlocked");
        }
        String previous = period.getStatus().name();
        period.setStatus(AccountingPeriodStatus.OPEN);
        period.setUnlockedAt(LocalDateTime.now());
        period.setUnlockedBy(SecurityUtils.getCurrentUser());
        period.setReopenReason(reason);
        AccountingPeriod saved = accountingPeriodRepository.save(period);

        auditLogService.record(FinancialPeriodEntityType.ACCOUNTING_PERIOD, saved.getId(),
                FinancialPeriodAction.UNLOCKED, previous, saved.getStatus().name(), reason);

        return toResponse(saved);
    }

    @Transactional
    public AccountingPeriodResponse close(Long id) {
        AccountingPeriod period = getEntity(id);
        if (period.getStatus() == AccountingPeriodStatus.CLOSED) {
            throw new BadRequestException("Period is already permanently closed");
        }
        String previous = period.getStatus().name();
        period.setStatus(AccountingPeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(SecurityUtils.getCurrentUser());
        AccountingPeriod saved = accountingPeriodRepository.save(period);

        auditLogService.record(FinancialPeriodEntityType.ACCOUNTING_PERIOD, saved.getId(),
                FinancialPeriodAction.CLOSED, previous, saved.getStatus().name(), null);

        return toResponse(saved);
    }

    /** Controlled reopening of a PERMANENTLY closed period. Requires a documented reason. */
    @Transactional
    public AccountingPeriodResponse reopen(Long id, String reason) {
        AccountingPeriod period = getEntity(id);
        if (period.getStatus() != AccountingPeriodStatus.CLOSED) {
            throw new BadRequestException("Only a permanently closed period can be reopened");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A reason is required to reopen a closed period");
        }
        String previous = period.getStatus().name();
        period.setStatus(AccountingPeriodStatus.OPEN);
        period.setUnlockedAt(LocalDateTime.now());
        period.setUnlockedBy(SecurityUtils.getCurrentUser());
        period.setReopenReason(reason);
        AccountingPeriod saved = accountingPeriodRepository.save(period);

        auditLogService.record(FinancialPeriodEntityType.ACCOUNTING_PERIOD, saved.getId(),
                FinancialPeriodAction.REOPENED, previous, saved.getStatus().name(), reason);

        return toResponse(saved);
    }

    /** Auto-locks every not-yet-closed period in a Financial Year, used by Year-End Closing. */
    @Transactional
    public void lockAllForFinancialYear(Long financialYearId) {
        for (AccountingPeriod period : accountingPeriodRepository.findByFinancialYearIdOrderByPeriodNumberAsc(financialYearId)) {
            if (period.getStatus() == AccountingPeriodStatus.OPEN) {
                String previous = period.getStatus().name();
                period.setStatus(AccountingPeriodStatus.LOCKED);
                period.setLockedAt(LocalDateTime.now());
                period.setLockedBy(SecurityUtils.getCurrentUser());
                accountingPeriodRepository.save(period);
                auditLogService.record(FinancialPeriodEntityType.ACCOUNTING_PERIOD, period.getId(),
                        FinancialPeriodAction.LOCKED, previous, period.getStatus().name(),
                        "Auto-locked by Year-End Closing");
            }
        }
    }

    private AccountingPeriodResponse toResponse(AccountingPeriod p) {
        User lockedBy = p.getLockedBy();
        User unlockedBy = p.getUnlockedBy();
        User closedBy = p.getClosedBy();
        return AccountingPeriodResponse.builder()
                .id(p.getId())
                .financialYearId(p.getFinancialYear().getId())
                .financialYearName(p.getFinancialYear().getName())
                .name(p.getName())
                .periodNumber(p.getPeriodNumber())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .isActive(p.isActive())
                .lockedAt(p.getLockedAt())
                .lockedByName(lockedBy != null ? fullName(lockedBy) : null)
                .unlockedAt(p.getUnlockedAt())
                .unlockedByName(unlockedBy != null ? fullName(unlockedBy) : null)
                .reopenReason(p.getReopenReason())
                .closedAt(p.getClosedAt())
                .closedByName(closedBy != null ? fullName(closedBy) : null)
                .build();
    }

    private String fullName(User user) {
        String name = (user.getFirstName() + " " + user.getLastName()).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
