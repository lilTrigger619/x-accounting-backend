package com.unionsg.xaccounting.service.reports.engine.adapter;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.engine.view.AccountAssignmentView;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;
import com.unionsg.xaccounting.service.reports.engine.view.TemplateReportSectionAdapter;
import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleAccountAssignmentView;
import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleReportSectionView;

import java.util.*;

/**
 * In-memory adapter that converts a {@link ReportTemplate} graph into the view model
 * expected by the report engine.
 */
public class TemplateReportAdapter {

    private TemplateReportAdapter() {
    }

    public static List<ReportSectionView> adapt(
            ReportTemplate template,
            List<ReportTemplateSection> sections,
            Map<Long, List<ReportTemplateSectionAccount>> accountsBySectionId
    ) {
        if (template == null) {
            return List.of();
        }
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }

        // Build lookup by parent/child using sectionCode.
        Map<String, ReportSectionView> sectionByCode = new HashMap<>();
        Map<String, List<String>> childrenByParentCode = new HashMap<>();

        // Pre-create section views (no children yet).
        for (ReportTemplateSection s : sections) {
            if (s == null || !s.isVisible()) continue;

            ReportSectionView view = new SimpleReportSectionView(
                    s.getId(),
                    s.getSectionCode(),
                    s.getTitle(),
                    s.getSectionType(),
                    s.getFormula(),
                    s.getDisplayOrder(),
                    s.getParentSection() != null ? s.getParentSection().getSectionCode() : null,
                    List.of()
            );

            sectionByCode.put(s.getSectionCode(), view);
        }

        // Build children lists (ordering preserved by displayOrder).
        List<ReportTemplateSection> sorted = sections.stream()
                .filter(s -> s != null && s.isVisible())
                .sorted(Comparator.comparingInt(ReportTemplateSection::getDisplayOrder))
                .toList();

        for (ReportTemplateSection s : sorted) {
            String parentCode = s.getParentSection() != null ? s.getParentSection().getSectionCode() : null;
            if (parentCode != null) {
                childrenByParentCode.computeIfAbsent(parentCode, k -> new ArrayList<>())
                        .add(s.getSectionCode());
            }
        }

        // Build AccountAssignmentView list per section code.
        Map<String, List<AccountAssignmentView>> assignmentsBySectionCode = new HashMap<>();
        for (ReportTemplateSection s : sorted) {
            List<ReportTemplateSectionAccount> ass = accountsBySectionId.getOrDefault(s.getId(), List.of());
            List<AccountAssignmentView> views = ass.stream()
                    .sorted(Comparator.comparingInt(ReportTemplateSectionAccount::getDisplayOrder))
                    .map(a -> toAssignment(a, s.getSectionCode()))
                    .toList();

            assignmentsBySectionCode.put(s.getSectionCode(), views);
        }

        // Now create final list of section views with hierarchical children.
        // Because ReportSectionView.children() is part of the formula/tree building in the future,
        // we fill it here.
        //
        // NOTE: ReportSectionView is currently an immutable contract; SimpleReportSectionView is a record.
        // We create new instances for updated children.

        Map<String, ReportSectionView> finalizedByCode = new HashMap<>();

        for (ReportTemplateSection s : sorted) {
            buildFinalized(s.getSectionCode(), childrenByParentCode, sectionByCode, assignmentsBySectionCode, finalizedByCode);
        }

        // Return all views (engine formula evaluation expects the flat list).
        // The tree resolver will reconstruct hierarchy via parentCode.
        return finalizedByCode.values().stream()
                .sorted(Comparator.comparingInt(v -> v.displayOrder()))
                .toList();
    }

    private static void buildFinalized(
            String code,
            Map<String, List<String>> childrenByParentCode,
            Map<String, ReportSectionView> baseByCode,
            Map<String, List<AccountAssignmentView>> assignmentsBySectionCode,
            Map<String, ReportSectionView> finalizedByCode
    ) {
        if (finalizedByCode.containsKey(code)) return;

        ReportSectionView base = baseByCode.get(code);
        if (base == null) return;

        List<String> childCodes = childrenByParentCode.getOrDefault(code, List.of());
        List<ReportSectionView> childViews = childCodes.stream()
                .map(finalizedByCode::get)
                .filter(Objects::nonNull)
                .toList();

        // Ensure children finalized
        for (String childCode : childCodes) {
            buildFinalized(childCode, childrenByParentCode, baseByCode, assignmentsBySectionCode, finalizedByCode);
        }
        childViews = childCodes.stream()
                .map(finalizedByCode::get)
                .filter(Objects::nonNull)
                .toList();

        // SimpleReportSectionView currently stores children as List<ReportSectionView> (not assignments).
        // Account assignments are handled through AccountAssignmentView list passed to aggregation.
        // So we just finalize the view hierarchy.
        ReportSectionView finalized = new SimpleReportSectionView(
                base.id(),
                base.code(),
                base.title(),
                base.sectionType(),
                base.formula(),
                base.displayOrder(),
                base.parentSectionCode(),
                childViews
        );

        finalizedByCode.put(code, finalized);
    }

    private static AccountAssignmentView toAssignment(ReportTemplateSectionAccount a, String sectionCode) {
        if (a == null || a.getAccount() == null) {
            return null;
        }
        AccountEntity acc = a.getAccount();
        return new SimpleAccountAssignmentView(
                acc.getId(),
                acc.getAccountId(),
                acc.getAccountName(),
                sectionCode
        );
    }
}

