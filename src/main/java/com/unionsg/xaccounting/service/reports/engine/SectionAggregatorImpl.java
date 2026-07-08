package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.reports.ReportSection;
import com.unionsg.xaccounting.repository.reports.ReportSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportSectionRepository;
import com.unionsg.xaccounting.repository.reports.ReportDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SectionAggregatorImpl implements SectionAggregator {


    private final ReportSectionRepository reportSectionRepository;
    private final ReportSectionAccountRepository reportSectionAccountRepository;
    private final JournalPostedBalanceRepository journalPostedBalanceRepository;



    @Override
    public Map<String, BigDecimal> aggregate(Long reportDefinitionId, LocalDate from, LocalDate to) {
        List<ReportSection> sections = reportSectionRepository.findByReportDefinitionId(reportDefinitionId);

        // Assigned accounts per section
        var assigned = sections.stream().collect(Collectors.toMap(
                ReportSection::getCode,
                s -> reportSectionAccountRepository.findByReportSectionId(s.getId())
                        .stream()
                        .map(rsa -> rsa.getAccount().getId())
                        .toList()
        ));

        // One DB aggregation for all accounts (group by accountId)
        List<Long> allAccountIds = assigned.values().stream().flatMap(List::stream).distinct().toList();
        if (allAccountIds.isEmpty()) {
            return assigned.keySet().stream().collect(Collectors.toMap(k -> k, k -> BigDecimal.ZERO));
        }

        Map<Long, BigDecimal> balancesByAccountId = journalPostedBalanceRepository.sumPostedBalancesByAccountId(allAccountIds, from, to);

        // Sum balances per section (leaf assigned accounts)
        return assigned.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                        .map(accId -> balancesByAccountId.getOrDefault(accId, BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        ));
    }
}

