package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.exception.TemplateNotFoundException;
import com.unionsg.xaccounting.service.reports.exception.TemplateSectionCodeAlreadyExistsException;
import com.unionsg.xaccounting.service.reports.exception.TemplateSectionCycleDetectedException;
import com.unionsg.xaccounting.service.reports.exception.TemplateSectionDisplayOrderConflictException;
import com.unionsg.xaccounting.service.reports.mapper.ReportTemplateSectionMapper;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportTemplateSectionServiceImpl implements ReportTemplateSectionService {

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateSectionRepository sectionRepository;
    private final ReportTemplateSectionMapper mapper;

    @Override
    @Transactional
    public ReportTemplateSectionResponseDto create(Long templateId, ReportTemplateSectionRequestDto request) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found for id: " + templateId));

        if (sectionRepository.findByReportTemplateIdAndSectionCode(templateId, request.sectionCode()).isPresent()) {
            throw new TemplateSectionCodeAlreadyExistsException("sectionCode already exists in template. code=" + request.sectionCode());
        }

        ReportTemplateSection parent = resolveParent(templateId, request.parentSectionId());

        enforceDisplayOrderNoDuplicate(templateId, parent == null ? null : parent.getId(), request.displayOrder(), null);

        ReportTemplateSection entity = mapper.toEntityForCreate(request, template, parent);

        // cycle check (when creating, cycle can only occur if parent points to itself, but still validate)
        if (hasCycle(entity, parent)) {
            throw new TemplateSectionCycleDetectedException("Cycle detected in parent-child structure");
        }

        ReportTemplateSection saved = sectionRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportTemplateSectionResponseDto getById(Long id) {
        ReportTemplateSection entity = sectionRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Section not found for id: " + id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportTemplateSectionResponseDto> listByTemplateId(Long templateId) {
        return sectionRepository.findByReportTemplateId(templateId).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ReportTemplateSectionResponseDto update(Long sectionId, ReportTemplateSectionRequestDto request) {
        ReportTemplateSection entity = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new TemplateNotFoundException("Section not found for id: " + sectionId));

        Long templateId = entity.getReportTemplate().getId();

        // Unique sectionCode within template
        sectionRepository.findByReportTemplateIdAndSectionCode(templateId, request.sectionCode())
                .filter(found -> !found.getId().equals(sectionId))
                .ifPresent(found -> {
                    throw new TemplateSectionCodeAlreadyExistsException("sectionCode already exists in template. code=" + request.sectionCode());
                });

        ReportTemplateSection parent = resolveParent(templateId, request.parentSectionId());

        enforceDisplayOrderNoDuplicate(templateId, parent == null ? null : parent.getId(), request.displayOrder(), sectionId);

        // cycle check
        if (parent != null) {
            if (wouldCreateCycle(entity, parent)) {
                throw new TemplateSectionCycleDetectedException("Cycle detected in parent-child structure");
            }
        }

        mapper.applyUpdates(entity, request, parent);

        ReportTemplateSection saved = sectionRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long sectionId) {
        // Note: requirement only mentions prevent deleting published templates (handled in template service)
        if (!sectionRepository.existsById(sectionId)) {
            throw new TemplateNotFoundException("Section not found for id: " + sectionId);
        }
        sectionRepository.deleteById(sectionId);
    }

    private ReportTemplateSection resolveParent(Long templateId, Long parentSectionId) {
        if (parentSectionId == null) return null;

        ReportTemplateSection parent = sectionRepository.findById(parentSectionId)
                .orElseThrow(() -> new TemplateNotFoundException("Parent section not found for id: " + parentSectionId));

        if (!parent.getReportTemplate().getId().equals(templateId)) {
            throw new IllegalArgumentException("parentSectionId does not belong to templateId");
        }

        return parent;
    }

    private void enforceDisplayOrderNoDuplicate(Long templateId, Long parentId, Integer displayOrder, Long excludingSectionId) {
        List<ReportTemplateSection> siblings;
        if (parentId == null) {
            siblings = sectionRepository.findByReportTemplateIdAndParentSectionIdIsNull(templateId);
        } else {
            siblings = sectionRepository.findByReportTemplateIdAndParentSectionId(templateId, parentId);
        }

        for (ReportTemplateSection sib : siblings) {
            if (excludingSectionId != null && sib.getId().equals(excludingSectionId)) continue;
            if (sib.getDisplayOrder().equals(displayOrder)) {
                throw new TemplateSectionDisplayOrderConflictException("displayOrder duplicated for same parent. displayOrder=" + displayOrder);
            }
        }
    }

    private boolean wouldCreateCycle(ReportTemplateSection current, ReportTemplateSection newParent) {
        // If we set current.parent = newParent, cycle exists if current appears in ancestor chain of newParent.
        ReportTemplateSection cursor = newParent;
        while (cursor != null) {
            if (cursor.getId().equals(current.getId())) {
                return true;
            }
            cursor = cursor.getParentSection();
        }
        return false;
    }

    private boolean hasCycle(ReportTemplateSection newChild, ReportTemplateSection parent) {
        if (parent == null) return false;
        return parent.getId() != null && newChild.getId() != null && wouldCreateCycle(newChild, parent);
    }
}

