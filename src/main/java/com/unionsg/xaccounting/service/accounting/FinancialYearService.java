package com.unionsg.xaccounting.service.accounting;

import com.unionsg.xaccounting.dto.accounting.CreateFinancialYearRequest;
import com.unionsg.xaccounting.dto.accounting.FinancialYearResponse;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.enums.FinancialPeriodAction;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import com.unionsg.xaccounting.enums.FinancialYearStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialYearService {

    private final FinancialYearRepository financialYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final FinancialPeriodAuditLogService auditLogService;

    @Transactional
    public FinancialYearResponse create(CreateFinancialYearRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        if (financialYearRepository.existsOverlapping(request.getStartDate(), request.getEndDate(), 0L)) {
            throw new BadRequestException("This date range overlaps an existing Financial Year");
        }

        FinancialYear fy = new FinancialYear();
        fy.setName(request.getName());
        fy.setStartDate(request.getStartDate());
        fy.setEndDate(request.getEndDate());
        fy.setStatus(FinancialYearStatus.DRAFT);
        FinancialYear saved = financialYearRepository.save(fy);

        if (request.getGenerateMonthlyPeriods() == null || request.getGenerateMonthlyPeriods()) {
            generateMonthlyPeriods(saved);
        }

        auditLogService.record(FinancialPeriodEntityType.FINANCIAL_YEAR, saved.getId(),
                FinancialPeriodAction.CREATED, null, saved.getStatus().name(), null);

        return toResponse(saved);
    }

    private void generateMonthlyPeriods(FinancialYear fy) {
        LocalDate cursor = fy.getStartDate();
        int periodNumber = 1;
        while (!cursor.isAfter(fy.getEndDate())) {
            LocalDate periodEnd = cursor.plusMonths(1).minusDays(1);
            if (periodEnd.isAfter(fy.getEndDate())) {
                periodEnd = fy.getEndDate();
            }

            AccountingPeriod period = new AccountingPeriod();
            period.setFinancialYear(fy);
            period.setPeriodNumber(periodNumber);
            period.setName(periodNumber + " - " + cursor.getMonth().name().substring(0, 1)
                    + cursor.getMonth().name().substring(1).toLowerCase() + " " + cursor.getYear());
            period.setStartDate(cursor);
            period.setEndDate(periodEnd);
            period.setStatus(AccountingPeriodStatus.OPEN);
            accountingPeriodRepository.save(period);

            cursor = periodEnd.plusDays(1);
            periodNumber++;
        }
    }

    @Transactional(readOnly = true)
    public List<FinancialYearResponse> getAll() {
        return financialYearRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FinancialYearResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public FinancialYearResponse getCurrent() {
        FinancialYear fy = financialYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No current Financial Year is set"));
        return toResponse(fy);
    }

    @Transactional(readOnly = true)
    public FinancialYear getEntity(Long id) {
        return financialYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial Year not found"));
    }

    @Transactional
    public FinancialYearResponse activate(Long id) {
        FinancialYear fy = getEntity(id);
        if (fy.getStatus() == FinancialYearStatus.CLOSED) {
            throw new BadRequestException("A closed Financial Year cannot be activated. Reopen it first.");
        }

        financialYearRepository.findByIsCurrentTrue().ifPresent(prev -> {
            if (!prev.getId().equals(fy.getId())) {
                prev.setCurrent(false);
                if (prev.getStatus() == FinancialYearStatus.ACTIVE) {
                    prev.setStatus(FinancialYearStatus.OPEN);
                }
                financialYearRepository.save(prev);
            }
        });

        String previousStatus = fy.getStatus().name();
        fy.setCurrent(true);
        fy.setStatus(FinancialYearStatus.ACTIVE);
        FinancialYear saved = financialYearRepository.save(fy);

        auditLogService.record(FinancialPeriodEntityType.FINANCIAL_YEAR, saved.getId(),
                FinancialPeriodAction.ACTIVATED, previousStatus, saved.getStatus().name(), null);

        return toResponse(saved);
    }

    public FinancialYearResponse toResponse(FinancialYear fy) {
        List<AccountingPeriod> periods = accountingPeriodRepository.findByFinancialYearIdOrderByPeriodNumberAsc(fy.getId());
        long locked = periods.stream().filter(p -> p.getStatus() == AccountingPeriodStatus.LOCKED).count();
        long closed = periods.stream().filter(p -> p.getStatus() == AccountingPeriodStatus.CLOSED).count();
        long open = periods.size() - locked - closed;

        User closedBy = fy.getClosedBy();
        User reopenedBy = fy.getReopenedBy();

        return FinancialYearResponse.builder()
                .id(fy.getId())
                .name(fy.getName())
                .startDate(fy.getStartDate())
                .endDate(fy.getEndDate())
                .status(fy.getStatus())
                .isCurrent(fy.isCurrent())
                .hasOpeningBalance(fy.isHasOpeningBalance())
                .periodCount(periods.size())
                .openPeriodCount((int) open)
                .lockedPeriodCount((int) locked)
                .closedPeriodCount((int) closed)
                .closedAt(fy.getClosedAt())
                .closedById(closedBy != null ? closedBy.getId() : null)
                .closedByName(closedBy != null ? fullName(closedBy) : null)
                .closingJournalId(fy.getClosingJournalId())
                .reopenedAt(fy.getReopenedAt())
                .reopenedByName(reopenedBy != null ? fullName(reopenedBy) : null)
                .reopenReason(fy.getReopenReason())
                .totalRevenue(fy.getTotalRevenue())
                .totalExpense(fy.getTotalExpense())
                .netProfitLoss(fy.getNetProfitLoss())
                .retainedEarningsMovement(fy.getRetainedEarningsMovement())
                .build();
    }

    private String fullName(User user) {
        String name = (user.getFirstName() + " " + user.getLastName()).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
