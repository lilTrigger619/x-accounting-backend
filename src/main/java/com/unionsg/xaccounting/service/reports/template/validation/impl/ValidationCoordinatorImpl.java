package com.unionsg.xaccounting.service.reports.template.validation.impl;

import com.unionsg.xaccounting.dto.reports.ReportTemplateValidationResponse;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.service.reports.template.validation.ValidationCoordinator;
import com.unionsg.xaccounting.service.reports.template.validation.ValidationResult;
import com.unionsg.xaccounting.service.reports.template.validation.StructureValidator;
import com.unionsg.xaccounting.service.reports.template.validation.FormulaValidatorAdapter;
import com.unionsg.xaccounting.service.reports.template.validation.AccountAssignmentValidator;
import com.unionsg.xaccounting.service.reports.template.validation.BusinessRuleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ValidationCoordinatorImpl implements ValidationCoordinator {

    private final ReportTemplateRepository templateRepository;

    private final StructureValidator structureValidator;
    private final FormulaValidatorAdapter formulaValidator;
    private final AccountAssignmentValidator accountAssignmentValidator;
    private final BusinessRuleValidator businessRuleValidator;

    @Override
    @Transactional(readOnly = true)
    public ReportTemplateValidationResponse validateTemplate(Long templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: id=" + templateId));

        ValidationResult result = new ValidationResult();

        structureValidator.validate(template, result);
        businessRuleValidator.validate(template, result);
        accountAssignmentValidator.validate(template, result);
        formulaValidator.validate(template, result);

        return result.toResponse();
    }
}

