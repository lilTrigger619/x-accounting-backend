package com.unionsg.xaccounting.service.reports.engine.view.impl;

import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import java.util.List;

public record SimpleReportSectionView(
        Long id,
        String sectionCode,
        String title,
        SectionType sectionType,
        String formula,
        Integer displayOrder,
        String parentSectionCode,
        List<ReportSectionView> children
) implements ReportSectionView {

    @Override
    public Long id() {
        return id;
    }

    @Override
    public String code() {
        return sectionCode;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public SectionType sectionType() {
        return sectionType;
    }

    @Override
    public String formula() {
        return formula;
    }

    @Override
    public Integer displayOrder() {
        return displayOrder;
    }

    @Override
    public String parentSectionCode() {
        return parentSectionCode;
    }

    @Override
    public List<ReportSectionView> children() {
        return children;
    }
}



