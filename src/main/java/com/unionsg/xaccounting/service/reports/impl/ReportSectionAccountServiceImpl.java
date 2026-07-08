package com.unionsg.xaccounting.service.reports.impl;

import com.unionsg.xaccounting.MapperLayer.reports.ReportSectionAccountMapper;
import com.unionsg.xaccounting.dto.reports.*;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportSection;
import com.unionsg.xaccounting.entity.reports.ReportSectionAccount;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportSectionRepository;
import com.unionsg.xaccounting.repository.reports.ReportSectionAccountQueriesRepository;
import com.unionsg.xaccounting.service.reports.ReportSectionAccountService;
import com.unionsg.xaccounting.service.reports.exception.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportSectionAccountServiceImpl implements ReportSectionAccountService {

    private final ReportSectionRepository reportSectionRepository;
    private final AccountRepository accountRepository;
    private final ReportSectionAccountRepository reportSectionAccountRepository;
    private final ReportSectionAccountQueriesRepository queriesRepository;
    private final ReportSectionAccountMapper mapper;

    @Override
    @Transactional
    public ReportSectionAccountResponseDto assignAccount(ReportSectionAccountRequestDto request) {
        ReportSection section = reportSectionRepository.findById(request.reportSectionId())
                .orElseThrow(() -> new ReportSectionAccountSectionNotFoundException(
                        "Report section not found: " + request.reportSectionId()));

        AccountEntity account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ReportSectionAccountAccountNotFoundException(
                        "Account not found: " + request.accountId()));

        if (queriesRepository.existsByReportSectionIdAndAccountId(section.getId(), account.getId())) {
            throw new ReportSectionAccountAlreadyAssignedException(
                    "Account already assigned to this report section");
        }

        ReportSectionAccount entity = ReportSectionAccount.builder()
                .reportSection(section)
                .account(account)
                .displayOrder(request.displayOrder())
                .build();

        ReportSectionAccount saved = reportSectionAccountRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeAssignment(Long reportSectionId, Long accountId) {
        ReportSection section = reportSectionRepository.findById(reportSectionId)
                .orElseThrow(() -> new ReportSectionAccountSectionNotFoundException(
                        "Report section not found: " + reportSectionId));

        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ReportSectionAccountAccountNotFoundException(
                        "Account not found: " + accountId));

        ReportSectionAccount assignment = queriesRepository.findByReportSectionIdAndAccountId(section.getId(), account.getId())
                .orElseThrow(() -> new ReportSectionAccountAssignmentNotFoundException(
                        "Assignment not found"));

        reportSectionAccountRepository.deleteById(assignment.getId());
    }

    @Override
    @Transactional
    public ReportSectionAccountListResponseDto listAssignments(Long reportSectionId) {
        if (!reportSectionRepository.existsById(reportSectionId)) {
            throw new ReportSectionAccountSectionNotFoundException(
                    "Report section not found: " + reportSectionId);
        }

        List<ReportSectionAccount> assignments = reportSectionAccountRepository.findByReportSectionId(reportSectionId);
        assignments.sort(Comparator.comparing(ReportSectionAccount::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));

        List<ReportSectionAccountResponseDto> content = assignments.stream()
                .map(mapper::toResponse)
                .toList();

        return new ReportSectionAccountListResponseDto(content);
    }

    @Override
    @Transactional
    public List<ReportSectionAccountResponseDto> bulkAssign(ReportSectionAccountBulkRequestDto request) {
        ReportSection section = reportSectionRepository.findById(request.reportSectionId())
                .orElseThrow(() -> new ReportSectionAccountSectionNotFoundException(
                        "Report section not found: " + request.reportSectionId()));

        // Basic pre-validation: ensure accounts exist and no duplicates in request by accountId
        List<ReportSectionAccountBulkItemDto> items = request.accounts();

        // Enforce no duplicates in payload
        items.stream()
                .collect(java.util.stream.Collectors.groupingBy(ReportSectionAccountBulkItemDto::accountId))
                .forEach((k, v) -> {
                    if (v.size() > 1) {
                        throw new ReportSectionAccountAlreadyAssignedException(
                                "Duplicate accountId in bulk request: " + k);
                    }
                });

        List<ReportSectionAccountResponseDto> results = items.stream()
                .map(item -> {
                    AccountEntity account = accountRepository.findById(item.accountId())
                            .orElseThrow(() -> new ReportSectionAccountAccountNotFoundException(
                                    "Account not found: " + item.accountId()));

                    if (queriesRepository.existsByReportSectionIdAndAccountId(section.getId(), account.getId())) {
                        throw new ReportSectionAccountAlreadyAssignedException(
                                "Account already assigned to this report section");
                    }

                    ReportSectionAccount entity = ReportSectionAccount.builder()
                            .reportSection(section)
                            .account(account)
                            .displayOrder(item.displayOrder())
                            .build();

                    return reportSectionAccountRepository.save(entity);
                })
                .map(mapper::toResponse)
                .toList();

        return results;
    }
}

