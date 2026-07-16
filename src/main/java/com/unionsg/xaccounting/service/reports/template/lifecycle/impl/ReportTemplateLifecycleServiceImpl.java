package com.unionsg.xaccounting.service.reports.template.lifecycle.impl;

import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;

import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.engine.FinancialReportEngine;
import com.unionsg.xaccounting.service.reports.engine.FormulaValidator;

import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import com.unionsg.xaccounting.repository.AccountRepository;



import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.reports.exception.ConcurrentTemplateModificationException;
import com.unionsg.xaccounting.service.reports.exception.InvalidTemplateStateException;
import com.unionsg.xaccounting.service.reports.exception.PublishValidationException;
import com.unionsg.xaccounting.service.reports.template.lifecycle.ReportTemplateLifecycleService;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleReportSectionView;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportTemplateLifecycleServiceImpl implements ReportTemplateLifecycleService {

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateSectionRepository sectionRepository;
    private final ReportTemplateSectionAccountRepository sectionAccountRepository;
    private final AccountRepository accountRepository;

    private final FinancialReportEngine financialReportEngine;
    private final FormulaValidator formulaValidator;

    private final com.unionsg.xaccounting.service.reports.template.audit.ReportTemplateAuditService auditService;


    @Override
    @Transactional(readOnly = true)
    public FinancialReportTreeResponseDto preview(Long templateId, LocalDate fromDate, LocalDate toDate) {

        ReportTemplate template = loadTemplateForPreview(templateId);
        auditService.record(templateId, com.unionsg.xaccounting.enums.ReportTemplateHistoryAction.PREVIEW);
        return financialReportEngine.generateFromTemplate(template, fromDate, toDate);


    }


    @Override
    @Transactional
    public void validate(Long templateId) {
        // method throws PublishValidationException if invalid
        doValidate(templateId);
    }

    @Override
    @Transactional
    public void publish(Long templateId, String updatedBy) {
        ReportTemplate draft = loadDraftTemplate(templateId);

        List<PublishValidationException.ValidationError> errors = validateInternal(draft.getId());
        if (!errors.isEmpty()) {
            System.out.println(errors.toString());
            throw new PublishValidationException("Template publish validation failed", errors);
        }

        createPublishedVersion(draft, updatedBy);
        auditService.record(templateId, com.unionsg.xaccounting.enums.ReportTemplateHistoryAction.PUBLISH);

    }

    @Override
    @Transactional
    public void archive(Long templateId, String updatedBy) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new InvalidTemplateStateException("Template not found for id=" + templateId));

        if (template.getStatus() != ReportTemplateStatus.PUBLISHED) {
            throw new InvalidTemplateStateException("Only PUBLISHED templates can be archived. Current=" + template.getStatus());
        }

        template.setStatus(ReportTemplateStatus.ARCHIVED);
        template.setUpdatedBy(updatedBy);
        templateRepository.save(template);
        auditService.record(templateId, com.unionsg.xaccounting.enums.ReportTemplateHistoryAction.ARCHIVE);


    }

    private ReportTemplate loadTemplateForPreview(Long templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new InvalidTemplateStateException("Template not found for id=" + templateId));

        if (template.getStatus() != ReportTemplateStatus.DRAFT) {
            throw new InvalidTemplateStateException("Preview allowed only for DRAFT templates. Current=" + template.getStatus());
        }
        return template;
    }

    private ReportTemplate loadDraftTemplate(Long templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new InvalidTemplateStateException("Template not found for id=" + templateId));

        if (template.getStatus() != ReportTemplateStatus.DRAFT) {
            throw new InvalidTemplateStateException("Publish allowed only for DRAFT templates. Current=" + template.getStatus());
        }
        return template;
    }

    private void doValidate(Long templateId) {
        List<PublishValidationException.ValidationError> errors = validateInternal(templateId);
        if (!errors.isEmpty()) {
            throw new PublishValidationException("Template validation failed", errors);
        }
    }

    private List<PublishValidationException.ValidationError> validateInternal(Long templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new InvalidTemplateStateException("Template not found for id=" + templateId));

        List<ReportTemplateSection> sections = sectionRepository.findByReportTemplateId(templateId);

        List<PublishValidationException.ValidationError> errors = new ArrayList<>();

        // Structure: at least one section
        if (sections == null || sections.isEmpty()) {
            errors.add(new PublishValidationException.ValidationError("STRUCTURE_EMPTY", "Template must contain at least one section", "template"));
            return errors;
        }

        // Structure: unique section codes
        Map<String, List<ReportTemplateSection>> byCode = new HashMap<>();
        for (ReportTemplateSection s : sections) {
            String code = s.getSectionCode();
            byCode.computeIfAbsent(code, k -> new ArrayList<>()).add(s);
        }
        for (var e : byCode.entrySet()) {
            if (e.getValue().size() > 1) {
                errors.add(new PublishValidationException.ValidationError(
                        "STRUCTURE_DUP_SECTION_CODE",
                        "Duplicate sectionCode in template: " + e.getKey(),
                        "sectionCode"
                ));
            }
        }

        // Structure: parent references exist + display order validity (no duplicates per parent)
        Set<Long> sectionIds = new HashSet<>();
        for (ReportTemplateSection s : sections) sectionIds.add(s.getId());

        for (ReportTemplateSection s : sections) {
            if (s.getParentSection() == null) continue;
            if (!sectionIds.contains(s.getParentSection().getId())) {
                errors.add(new PublishValidationException.ValidationError(
                        "STRUCTURE_MISSING_PARENT",
                        "Parent section reference not found for section: " + s.getSectionCode(),
                        s.getSectionCode()
                ));
            }
        }

        // displayOrder duplicates per parent
        Map<String, Map<Integer, Integer>> seenOrders = new HashMap<>();
        for (ReportTemplateSection s : sections) {
            String parentKey = s.getParentSection() == null ? "null" : String.valueOf(s.getParentSection().getId());
            seenOrders.putIfAbsent(parentKey, new HashMap<>());
            Map<Integer, Integer> m = seenOrders.get(parentKey);
            m.put(s.getDisplayOrder(), m.getOrDefault(s.getDisplayOrder(), 0) + 1);
        }
        for (var parentEntry : seenOrders.entrySet()) {
            for (var orderEntry : parentEntry.getValue().entrySet()) {
                if (orderEntry.getValue() > 1) {
                    errors.add(new PublishValidationException.ValidationError(
                            "STRUCTURE_DISPLAY_ORDER_CONFLICT",
                            "Duplicate displayOrder among sibling sections. parent=" + parentEntry.getKey() + ", order=" + orderEntry.getKey(),
                            "displayOrder"
                    ));
                }
            }
        }

        // Formula + accounts validations rely on engine views; easiest is to build a minimal view map
        // FormulaValidator expects ReportSectionView per code + baseByCode + sectionsInGraph.
        Map<String, ReportSectionView> allByCode = new HashMap<>();
        List<ReportSectionView> sectionsInGraph = new ArrayList<>();
        Map<String, BigDecimal> baseByCode = new HashMap<>();

        // Build section views. FormulaValidator only needs code/formula + graph traversal; still provide required fields.
        for (ReportTemplateSection s : sections) {
            String parentCode = s.getParentSection() == null ? null : s.getParentSection().getSectionCode();
            ReportSectionView view = new SimpleReportSectionView(
                    s.getId(),
                    s.getSectionCode(),
                    s.getTitle(),
                    s.getSectionType(),
                    s.getFormula(),
                    s.getDisplayOrder(),
                    parentCode,
                    List.of()
            );

            allByCode.put(s.getSectionCode(), view);
            sectionsInGraph.add(view);
            baseByCode.put(s.getSectionCode(), BigDecimal.ZERO);
        }


        // Formula validation for each section with formula
        for (ReportTemplateSection s : sections) {
            try {
                formulaValidator.validate(s.getSectionCode(), s.getFormula(), allByCode, sectionsInGraph, baseByCode);
            } catch (RuntimeException ex) {
                errors.add(new PublishValidationException.ValidationError(
                        "FORMULA_INVALID",
                        ex.getMessage(),
                        s.getSectionCode()
                ));
            }
        }

        // Account validation
        for (ReportTemplateSection section : sections) {
            List<ReportTemplateSectionAccount> assignments = sectionAccountRepository.findByReportTemplateSectionId(section.getId());

            // required sections not empty
            if (section.getSectionType() == SectionType.SECTION) {
                if (assignments == null || assignments.isEmpty()) {
                    errors.add(new PublishValidationException.ValidationError(
                            "ACCOUNT_REQUIRED_EMPTY",
                            "Section must have at least one assigned account. sectionCode=" + section.getSectionCode(),
                            section.getSectionCode()
                    ));
                }
            }

            // duplicates in payload
            if (assignments != null) {
                Set<Long> accountIds = new HashSet<>();
                for (ReportTemplateSectionAccount a : assignments) {
                    Long accountId = a.getAccount().getId();
                    if (!accountIds.add(accountId)) {
                        errors.add(new PublishValidationException.ValidationError(
                                "ACCOUNT_DUPLICATE_ASSIGNMENT",
                                "Duplicate account assignment in section. sectionCode=" + section.getSectionCode() + ", accountId=" + accountId,
                                section.getSectionCode()
                        ));
                    }
                }

                // ensure accounts exist
                for (ReportTemplateSectionAccount a : assignments) {
                    Long accountId = a.getAccount().getId();
                    if (!accountRepository.existsById(accountId)) {
                        errors.add(new PublishValidationException.ValidationError(
                                "ACCOUNT_NOT_FOUND",
                                "Assigned account not found. sectionCode=" + section.getSectionCode() + ", accountId=" + accountId,
                                section.getSectionCode()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public com.unionsg.xaccounting.dto.reports.ReportTemplateDto clone(Long templateId, String updatedBy) {
        ReportTemplate source = templateRepository.findById(templateId)
                .orElseThrow(() -> new InvalidTemplateStateException("Template not found for id=" + templateId));

        String sourceTemplateCode = source.getTemplateCode();

        String newTemplateCode = generateNextCopyTemplateCode(sourceTemplateCode);
        int nextVersion = 1;

        // Create brand-new draft (do not copy audit fields)
        ReportTemplate draft = ReportTemplate.builder()
                .templateCode(newTemplateCode)
                .templateName(source.getTemplateName())
                .description(source.getDescription())
                .category(source.getCategory())
                .status(ReportTemplateStatus.DRAFT)
                .version(nextVersion)
                .isSystemTemplate(source.isSystemTemplate())
                .updatedBy(updatedBy)
                .createdBy(null)
                .createdDate(null)
                .updatedDate(null)
                .build();

        ReportTemplate savedDraft = templateRepository.save(draft);

        // Copy sections (two-pass parent rebuild)
        List<ReportTemplateSection> sourceSections = sectionRepository.findByReportTemplateId(source.getId());
        Map<Long, ReportTemplateSection> oldToNew = new HashMap<>();

        for (ReportTemplateSection old : sourceSections) {
            ReportTemplateSection created = ReportTemplateSection.builder()
                    .reportTemplate(savedDraft)
                    .sectionCode(old.getSectionCode())
                    .title(old.getTitle())
                    .displayOrder(old.getDisplayOrder())
                    .sectionType(old.getSectionType())
                    .formula(old.getFormula())
                    .visible(old.isVisible())
                    .expandedByDefault(old.isExpandedByDefault())
                    .build();
            ReportTemplateSection saved = sectionRepository.save(created);
            oldToNew.put(old.getId(), saved);
        }

        for (ReportTemplateSection old : sourceSections) {
            if (old.getParentSection() == null) continue;
            ReportTemplateSection newSection = oldToNew.get(old.getId());
            ReportTemplateSection newParent = oldToNew.get(old.getParentSection().getId());
            newSection.setParentSection(newParent);
            sectionRepository.save(newSection);
        }

        // Copy section account assignments
        for (ReportTemplateSection old : sourceSections) {
            ReportTemplateSection newSection = oldToNew.get(old.getId());
            List<ReportTemplateSectionAccount> assignments = sectionAccountRepository.findByReportTemplateSectionId(old.getId());
            if (assignments == null || assignments.isEmpty()) continue;

            List<ReportTemplateSectionAccount> newAssignments = new ArrayList<>();
            for (ReportTemplateSectionAccount a : assignments) {
                ReportTemplateSectionAccount na = ReportTemplateSectionAccount.builder()
                        .reportTemplateSection(newSection)
                        .account(a.getAccount())
                        .displayOrder(a.getDisplayOrder())
                        .build();
                newAssignments.add(na);
            }
            sectionAccountRepository.saveAll(newAssignments);
        }

        auditService.record(savedDraft.getId(), com.unionsg.xaccounting.enums.ReportTemplateHistoryAction.CLONE,
                Map.of(
                        "sourceTemplateId", templateId,
                        "sourceTemplateCode", sourceTemplateCode,
                        "newTemplateCode", newTemplateCode
                ));

        return toDto(savedDraft);

    }

    private String generateNextCopyTemplateCode(String sourceTemplateCode) {
        // Expected pattern: {BASE}_COPY_{NNN}
        // If templateCode already ends with COPY_{NNN}, base that out; otherwise treat entire string as base.
        String base;
        String prefix;
        if (sourceTemplateCode != null && sourceTemplateCode.matches(".*_COPY_\\d{3}$")) {
            base = sourceTemplateCode.substring(0, sourceTemplateCode.lastIndexOf("_COPY_"));
            prefix = base + "_COPY_";
        } else {
            prefix = sourceTemplateCode + "_COPY_";
            base = sourceTemplateCode;
        }

        int max = 0;
        for (ReportTemplate t : templateRepository.findAll()) {
            String code = t.getTemplateCode();
            if (code == null || !code.startsWith(prefix)) continue;
            String suffix = code.substring(prefix.length());
            if (!suffix.matches("\\d+")) continue;
            int n = Integer.parseInt(suffix);
            if (n > max) max = n;
        }
        int next = max + 1;
        return prefix + String.format("%03d", next);
    }

    private com.unionsg.xaccounting.dto.reports.ReportTemplateDto toDto(ReportTemplate t) {
        return new com.unionsg.xaccounting.dto.reports.ReportTemplateDto(
                t.getId(),
                t.getTemplateCode(),
                t.getTemplateName(),
                t.getDescription(),
                t.getCategory(),
                t.getStatus(),
                t.getVersion(),
                t.isSystemTemplate(),
                t.getCreatedBy(),
                t.getCreatedDate(),
                t.getUpdatedBy(),
                t.getUpdatedDate()
        );
    }

    private void createPublishedVersion(ReportTemplate draft, String updatedBy) {

        String templateCode = draft.getTemplateCode();

        // Determine next version among PUBLISHED versions with same templateCode
        int currentMax = templateRepository.findAll().stream()
                .filter(t -> Objects.equals(t.getTemplateCode(), templateCode) && t.getStatus() == ReportTemplateStatus.PUBLISHED)
                .map(ReportTemplate::getVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        int nextVersion = currentMax + 1;

        ReportTemplate published = ReportTemplate.builder()
                .templateCode(draft.getTemplateCode())
                .templateName(draft.getTemplateName())
                .description(draft.getDescription())
                .category(draft.getCategory())
                .status(ReportTemplateStatus.PUBLISHED)
                .version(nextVersion)
                .isSystemTemplate(draft.isSystemTemplate())
                .createdBy(draft.getCreatedBy())
                .createdDate(draft.getCreatedDate())
                .updatedBy(updatedBy)
                .updatedDate(LocalDateTime.now())
                .build();

        ReportTemplate savedPublished = templateRepository.save(published);

        // Copy sections (two-pass to rebuild parent links)
        List<ReportTemplateSection> draftSections = sectionRepository.findByReportTemplateId(draft.getId());
        Map<Long, ReportTemplateSection> oldToNew = new HashMap<>();

        // pass1 create section entities without parent refs
        for (ReportTemplateSection old : draftSections) {
            ReportTemplateSection created = ReportTemplateSection.builder()
                    .reportTemplate(savedPublished)
                    .sectionCode(old.getSectionCode())
                    .title(old.getTitle())
                    .displayOrder(old.getDisplayOrder())
                    .sectionType(old.getSectionType())
                    .formula(old.getFormula())
                    .visible(old.isVisible())
                    .expandedByDefault(old.isExpandedByDefault())
                    .build();
            ReportTemplateSection saved = sectionRepository.save(created);
            oldToNew.put(old.getId(), saved);
        }

        // pass2 set parents
        for (ReportTemplateSection old : draftSections) {
            if (old.getParentSection() == null) continue;
            ReportTemplateSection newSection = oldToNew.get(old.getId());
            ReportTemplateSection newParent = oldToNew.get(old.getParentSection().getId());
            newSection.setParentSection(newParent);
            sectionRepository.save(newSection);
        }

        // Copy section accounts
        for (ReportTemplateSection old : draftSections) {
            ReportTemplateSection newSection = oldToNew.get(old.getId());
            List<ReportTemplateSectionAccount> assignments = sectionAccountRepository.findByReportTemplateSectionId(old.getId());
            if (assignments == null || assignments.isEmpty()) continue;

            List<ReportTemplateSectionAccount> newAssignments = new ArrayList<>();
            for (ReportTemplateSectionAccount a : assignments) {
                ReportTemplateSectionAccount na = ReportTemplateSectionAccount.builder()
                        .reportTemplateSection(newSection)
                        .account(a.getAccount())
                        .displayOrder(a.getDisplayOrder())
                        .build();
                newAssignments.add(na);
            }
            sectionAccountRepository.saveAll(newAssignments);
        }
    }
}

