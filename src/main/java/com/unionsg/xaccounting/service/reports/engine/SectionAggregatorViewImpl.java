package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportSection;
import com.unionsg.xaccounting.service.reports.engine.view.AccountAssignmentView;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SectionAggregatorViewImpl implements SectionAggregatorView {

    private final JournalPostedBalanceRepository journalPostedBalanceRepository;

    @Override
    public Map<String, BigDecimal> aggregate(List<ReportSectionView> sections,
                                              List<AccountAssignmentView> assignments,
                                              LocalDate from,
                                              LocalDate to) {

        if (sections == null || sections.isEmpty()) {
            return Map.of();
        }

        // assignments are expected to include the target section code
        // We'll compute balancesBySectionCode by summing balancesByAccountId for each assignment.
        //
        // Since AccountAssignmentView currently only exposes accountId(), we need a mapping from
        // assignments to section code. That information is available on the concrete view implementation
        // via method name getSectionCode() / sectionCode() depending on interface.
        //
        // To keep changes minimal, we support two common method names using casting.

        Map<String, List<Long>> accountsBySectionCode = new HashMap<>();
        Set<Long> allAccountIds = new HashSet<>();

        for (AccountAssignmentView a : assignments) {
            if (a == null) continue;

            String sectionCode = a.sectionCode();

            if (sectionCode == null) {


                // If we cannot resolve section code from assignment view, we cannot aggregate.
                // Return empty balances rather than wrong results.
                return Map.of();
            }

            accountsBySectionCode.computeIfAbsent(sectionCode, k -> new ArrayList<>()).add(a.accountId());
            allAccountIds.add(a.accountId());
        }

        if (allAccountIds.isEmpty()) {
            // ensure all section codes exist
            return sections.stream()
                    .collect(Collectors.toMap(ReportSectionView::code, s -> BigDecimal.ZERO));
        }


        Map<Long, BigDecimal> balancesByAccountId = journalPostedBalanceRepository.sumPostedBalancesByAccountId(
                allAccountIds.stream().toList(),
                from,
                to
        );

        Map<String, BigDecimal> result = new HashMap<>();

        for (ReportSectionView s : sections) {
            String code = s.code();

            List<Long> accIds = accountsBySectionCode.getOrDefault(code, List.of());
            BigDecimal sum = accIds.stream()
                    .map(accId -> balancesByAccountId.getOrDefault(accId, BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(code, sum);
        }

        return result;
    }
}

