package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountResponseDto;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.reports.exception.TemplateNotFoundException;
import com.unionsg.xaccounting.service.reports.exception.TemplateSectionAccountAlreadyAssignedException;
import com.unionsg.xaccounting.service.reports.exception.TemplateSectionAccountNotFoundException;
import com.unionsg.xaccounting.service.reports.mapper.ReportTemplateSectionAccountMapper;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateSectionAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportTemplateSectionAccountServiceImpl implements ReportTemplateSectionAccountService {

    private final ReportTemplateSectionRepository sectionRepository;
    private final ReportTemplateSectionAccountRepository assignmentRepository;
    private final AccountRepository accountRepository;
    private final ReportTemplateSectionAccountMapper mapper;

    @Override
    @Transactional
    public ReportTemplateSectionAccountResponseDto assign(Long sectionId, ReportTemplateSectionAccountRequestDto request) {
        ReportTemplateSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new TemplateNotFoundException("Template section not found: " + sectionId));

        if (section.getReportTemplate().getStatus() != com.unionsg.xaccounting.enums.ReportTemplateStatus.DRAFT) {
            throw new com.unionsg.xaccounting.service.reports.exception.InvalidTemplateStateException("Only DRAFT templates are editable. Current=" + section.getReportTemplate().getStatus());
        }


        if (request.reportTemplateSectionId() != null && !request.reportTemplateSectionId().equals(sectionId)) {
            throw new IllegalArgumentException("reportTemplateSectionId mismatch with path id");
        }

        Long accountId = request.accountId();

        if (assignmentRepository.existsByReportTemplateSectionIdAndAccountId(sectionId, accountId)) {
            throw new TemplateSectionAccountAlreadyAssignedException(
                    "Account already assigned to section. sectionId=" + sectionId + " accountId=" + accountId);
        }

        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new TemplateNotFoundException("Account not found for id: " + accountId));

        ReportTemplateSectionAccount entity = mapper.toEntityForCreate(request, section, account);
        ReportTemplateSectionAccount saved = assignmentRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void remove(Long sectionId, Long accountId) {
        ReportTemplateSectionAccount existing = assignmentRepository
                .findByReportTemplateSectionIdAndAccountId(sectionId, accountId)
                .orElseThrow(() -> new TemplateSectionAccountNotFoundException(
                        "Assignment not found. sectionId=" + sectionId + " accountId=" + accountId));

        if (existing.getReportTemplateSection().getReportTemplate().getStatus() != com.unionsg.xaccounting.enums.ReportTemplateStatus.DRAFT) {
            throw new com.unionsg.xaccounting.service.reports.exception.InvalidTemplateStateException("Only DRAFT templates are editable. Current=" + existing.getReportTemplateSection().getReportTemplate().getStatus());
        }

        assignmentRepository.delete(existing);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ReportTemplateSectionAccountResponseDto> listBySectionId(Long sectionId) {
        return assignmentRepository.findByReportTemplateSectionIdOrderByDisplayOrderAsc(sectionId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}

