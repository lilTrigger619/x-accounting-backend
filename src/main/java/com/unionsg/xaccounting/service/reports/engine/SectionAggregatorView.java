package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.service.reports.engine.view.AccountAssignmentView;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * View-based section aggregation.
 *
 * This overload is introduced for template execution.
 * Runtime (ReportDefinition) aggregation remains intact.
 */
public interface SectionAggregatorView {

    Map<String, BigDecimal> aggregate(
            List<ReportSectionView> sections,
            List<AccountAssignmentView> assignments,
            LocalDate from,
            LocalDate to
    );
}

