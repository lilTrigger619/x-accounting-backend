package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.dto.reports.ReportVersionDetailDto;
import com.unionsg.xaccounting.dto.reports.ReportVersionDto;
import com.unionsg.xaccounting.dto.reports.RollbackResponseDto;

import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.service.reports.template.VersionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VersionHistoryServiceImpl implements VersionHistoryService {

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateSectionRepository sectionRepository;
    private final ReportTemplateSectionAccountRepository sectionAccountRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReportVersionDto> getVersions(Long templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BadRequestException("Template not found for id=" + templateId));

        String templateCode = template.getTemplateCode();

        // Spec: every published version for same templateCode, order descending by version
        // Repository doesn't currently provide a direct query; use findAll safely.
        return templateRepository.findAll().stream()
                .filter(t -> Objects.equals(t.getTemplateCode(), templateCode))
                .filter(t -> t.getStatus() == ReportTemplateStatus.PUBLISHED)
                .sorted(Comparator.comparing(ReportTemplate::getVersion, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(t -> new ReportVersionDto(
                        t.getId(),
                        t.getVersion(),
                        t.getStatus(),
                        t.getUpdatedBy(),
                        t.getUpdatedDate()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportVersionDetailDto getVersion(Long versionId) {
        ReportTemplate template = templateRepository.findById(versionId)
                .orElseThrow(() -> new BadRequestException("Version not found for id=" + versionId));

        List<ReportTemplateSection> sections = sectionRepository.findByReportTemplateId(versionId);
        List<ReportTemplateSectionAccount> assignments;

        List<ReportVersionDetailDto.SectionDto> sectionDtos = new ArrayList<>();
        List<ReportVersionDetailDto.FormulaDto> formulaDtos = new ArrayList<>();
        List<ReportVersionDetailDto.AccountAssignmentDto> assignmentDtos = new ArrayList<>();

        if (sections != null) {
            for (ReportTemplateSection s : sections) {
                String parentCode = s.getParentSection() == null ? null : s.getParentSection().getSectionCode();

                sectionDtos.add(new ReportVersionDetailDto.SectionDto(
                        s.getId(),
                        s.getSectionCode(),
                        s.getTitle(),
                        s.getDisplayOrder(),
                        s.getSectionType(),
                        s.getFormula(),
                        s.isVisible(),
                        s.isExpandedByDefault(),
                        parentCode
                ));

                if (s.getFormula() != null && !s.getFormula().isBlank()) {
                    formulaDtos.add(new ReportVersionDetailDto.FormulaDto(s.getSectionCode(), s.getFormula()));
                }
            }
        }

        if (sections != null) {
            for (ReportTemplateSection sec : sections) {
                assignments = sectionAccountRepository.findByReportTemplateSectionId(sec.getId());
                if (assignments == null) continue;
                for (ReportTemplateSectionAccount a : assignments) {
                    assignmentDtos.add(new ReportVersionDetailDto.AccountAssignmentDto(
                            sec.getSectionCode(),
                            a.getAccount().getId(),
                            a.getDisplayOrder()
                    ));
                }
            }
        }

        return new ReportVersionDetailDto(
                template.getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getDescription(),
                template.getCategory(),
                template.getStatus(),
                template.getVersion(),
                template.isSystemTemplate(),
                template.getUpdatedBy(),
                template.getUpdatedDate(),
                sectionDtos,
                formulaDtos,
                assignmentDtos
        );
    }

    @Override
    @Transactional
    public RollbackResponseDto rollback(Long versionId, String updatedBy) {
        ReportTemplate source = templateRepository.findById(versionId)
                .orElseThrow(() -> new BadRequestException("Version not found for id=" + versionId));

        if (source.getStatus() != ReportTemplateStatus.PUBLISHED) {
            throw new BadRequestException("Only PUBLISHED versions can be rolled back. Current=" + source.getStatus());
        }

        String templateCode = source.getTemplateCode();

        // Next draft version number for same templateCode
        int currentMax = templateRepository.findAll().stream()
                .filter(t -> Objects.equals(t.getTemplateCode(), templateCode))
                .map(ReportTemplate::getVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        int nextVersion = currentMax + 1;

        ReportTemplate draft = ReportTemplate.builder()
                .templateCode(source.getTemplateCode())
                .templateName(source.getTemplateName())
                .description(source.getDescription())
                .category(source.getCategory())
                .status(ReportTemplateStatus.DRAFT)
                .version(nextVersion)
                .isSystemTemplate(source.isSystemTemplate())
                .createdBy(source.getCreatedBy())
                .createdDate(source.getCreatedDate())
                .updatedBy(updatedBy)
                .updatedDate(LocalDateTime.now())
                .build();

        ReportTemplate savedDraft = templateRepository.save(draft);

        // Copy sections (IDs not copied; hierarchy rebuilt via two-pass)
        List<ReportTemplateSection> sourceSections = sectionRepository.findByReportTemplateId(source.getId());
        if (sourceSections == null) return new RollbackResponseDto(
                savedDraft.getId(),
                savedDraft.getVersion(),
                savedDraft.getStatus(),
                savedDraft.getCreatedBy(),
                savedDraft.getCreatedDate()
        );

        var oldToNewSection = new java.util.HashMap<Long, ReportTemplateSection>();

        // pass1 create sections without parent
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
            oldToNewSection.put(old.getId(), saved);
        }

        // pass2 set parents
        for (ReportTemplateSection old : sourceSections) {
            if (old.getParentSection() == null) continue;
            ReportTemplateSection newSection = oldToNewSection.get(old.getId());
            ReportTemplateSection newParent = oldToNewSection.get(old.getParentSection().getId());
            newSection.setParentSection(newParent);
            sectionRepository.save(newSection);
        }

        // Copy section account assignments
        for (ReportTemplateSection old : sourceSections) {
            ReportTemplateSection newSection = oldToNewSection.get(old.getId());
            List<ReportTemplateSectionAccount> sourceAssignments = sectionAccountRepository.findByReportTemplateSectionId(old.getId());
            if (sourceAssignments == null || sourceAssignments.isEmpty()) continue;

            List<ReportTemplateSectionAccount> newAssignments = new ArrayList<>();
            for (ReportTemplateSectionAccount a : sourceAssignments) {
                newAssignments.add(ReportTemplateSectionAccount.builder()
                        .reportTemplateSection(newSection)
                        .account(a.getAccount())
                        .displayOrder(a.getDisplayOrder())
                        .build());
            }
            sectionAccountRepository.saveAll(newAssignments);
        }

        return new RollbackResponseDto(
                savedDraft.getId(),
                savedDraft.getVersion(),
                savedDraft.getStatus(),
                updatedBy,
                savedDraft.getCreatedDate()
        );
    }
}

