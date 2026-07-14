package com.unionsg.xaccounting.service.reports.engine.view;

import com.unionsg.xaccounting.enums.SectionType;

import java.util.List;

public interface ReportSectionView {

    Long id();

    String code();

    String title();

    SectionType sectionType();

    String formula();

    Integer displayOrder();

    /**
     * Parent section code (null when no parent)
     */
    String parentSectionCode();

    /**
     * Optional; engine may ignore this and compute child relationships from parentSectionCode.
     */
    List<ReportSectionView> children();
}

