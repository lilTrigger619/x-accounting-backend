package com.unionsg.xaccounting.service.reports.engine.view.impl;

import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import java.util.Collections;
import java.util.List;

public record SimpleReportSectionView(
        Long id,
        String code,
        String title,
        SectionType sectionType,
        String formula,
        Integer displayOrder,
        String parentSectionCode,
        List<ReportSectionView> children
) implements ReportSectionView {

    public SimpleReportSectionView {
        if (code != null) code = code.toUpperCase();
        if (children == null) children = Collections.emptyList();
    }
}

