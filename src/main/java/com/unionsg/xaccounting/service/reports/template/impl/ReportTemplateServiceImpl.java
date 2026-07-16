package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.dto.reports.ReportTemplateRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateResponseDto;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.service.reports.exception.TemplateCodeAlreadyExistsException;
import com.unionsg.xaccounting.service.reports.exception.TemplateNotFoundException;
import com.unionsg.xaccounting.service.reports.exception.TemplatePublishedDeletionException;
import com.unionsg.xaccounting.service.reports.mapper.ReportTemplateMapper;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportTemplateServiceImpl implements ReportTemplateService {

    private final ReportTemplateRepository repository;
    private final ReportTemplateMapper mapper;

    @Override
    @Transactional
    public ReportTemplateResponseDto create(ReportTemplateRequestDto request) {
        if (repository.existsByTemplateCode(request.templateCode())) {
            throw new TemplateCodeAlreadyExistsException("templateCode already exists: " + request.templateCode());
        }

        var principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String createdBy = principal instanceof com.unionsg.xaccounting.security.auth.UserPrincipal up ? up.getUsername() : principal.toString();
        ReportTemplate entity = mapper.toEntityForCreate(request, createdBy);
        entity.setVersion(request.version());


        ReportTemplate saved = repository.save(entity);
        System.out.println("After saving");
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportTemplateResponseDto getById(Long id) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found for id: " + id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportTemplateResponseDto getByTemplateCode(String templateCode) {
        ReportTemplate entity = repository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found for templateCode: " + templateCode));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportTemplateResponseDto> listAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ReportTemplateResponseDto update(Long id, ReportTemplateRequestDto request) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found for id: " + id));

        // templateCode uniqueness (allow same value for same entity)
        if (!entity.getTemplateCode().equals(request.templateCode()) && repository.existsByTemplateCode(request.templateCode())) {
            throw new TemplateCodeAlreadyExistsException("templateCode already exists: " + request.templateCode());
        }

        var principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String updatedBy = principal instanceof com.unionsg.xaccounting.security.auth.UserPrincipal up ? up.getUsername() : principal.toString();

        mapper.applyUpdates(entity, request, updatedBy);


        ReportTemplate saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found for id: " + id));

        if (entity.getStatus() == ReportTemplateStatus.PUBLISHED) {
            throw new TemplatePublishedDeletionException("Cannot delete published template. id=" + id);
        }

        repository.delete(entity);
    }

    @Override
    @Transactional
    public ReportTemplateResponseDto setStatus(Long id, ReportTemplateStatus status) {

        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found for id: " + id));

        var principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String updatedBy = principal instanceof com.unionsg.xaccounting.security.auth.UserPrincipal up ? up.getUsername() : principal.toString();

        entity.setStatus(status);
        entity.setUpdatedBy(updatedBy);



        ReportTemplate saved = repository.save(entity);
        return mapper.toResponse(saved);
    }
}

