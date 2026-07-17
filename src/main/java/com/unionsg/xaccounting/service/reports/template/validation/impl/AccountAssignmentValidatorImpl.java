package com.unionsg.xaccounting.service.reports.template.validation.impl;

import com.unionsg.xaccounting.dto.reports.ValidationErrorDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.ReportTemplateValidationSeverity;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.reports.template.validation.AccountAssignmentValidator;
import com.unionsg.xaccounting.service.reports.template.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountAssignmentValidatorImpl implements AccountAssignmentValidator {

    private final ReportTemplateSectionRepository sectionRepository;
    private final ReportTemplateSectionAccountRepository sectionAccountRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public void validate(ReportTemplate template, ValidationResult result) {
        List<ReportTemplateSection> sections = sectionRepository.findByReportTemplateId(template.getId());
        if (sections == null || sections.isEmpty()) return;

        for (ReportTemplateSection section : sections) {
            List<ReportTemplateSectionAccount> assignments = sectionAccountRepository.findByReportTemplateSectionId(section.getId());
            boolean hasAssignments = assignments != null && !assignments.isEmpty();

            // Duplicate account assignments + existence checks
            if (assignments != null && !assignments.isEmpty()) {
                Set<Long> seenAccountIds = new HashSet<>();
                for (ReportTemplateSectionAccount a : assignments) {
                    Long accountId = a.getAccount().getId();

                    if (!seenAccountIds.add(accountId)) {
                        result.addError(new ValidationErrorDto(
                                "ACCOUNT_DUPLICATE_ASSIGNMENT",
                                "Duplicate account assignment in section '" + section.getSectionCode() + "'.",
                                section.getSectionCode(),
                                "Account assignments",
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }

                    if (!accountRepository.existsById(accountId)) {
                        result.addError(new ValidationErrorDto(
                                "ACCOUNT_NOT_FOUND",
                                "Assigned account not found (accountId=" + accountId + ").",
                                section.getSectionCode(),
                                "Account assignments",
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                }
            }

            SectionType type = section.getSectionType();
            if (type == null) continue;

            switch (type) {
                case SECTION -> {
                    // Legacy SECTION behaves like DETAIL: must have at least one account.
                    if (!hasAssignments) {
                        result.addError(new ValidationErrorDto(
                                "DETAIL_REQUIRES_ACCOUNT",
                                "The '" + section.getSectionCode() + "' section must contain at least one assigned account.",
                                section.getSectionCode(),
                                section.getSectionCode(),
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                }
                case GROUP -> {

                    if (hasAssignments) {
                        result.addError(new ValidationErrorDto(
                                "GROUP_CONTAINS_ACCOUNTS",
                                "The '" + section.getSectionCode() + "' section must NOT contain account assignments because it is a GROUP (structural heading).",
                                section.getSectionCode(),
                                section.getSectionCode(),
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                }
                case DETAIL -> {
                    if (!hasAssignments) {
                        result.addError(new ValidationErrorDto(
                                "DETAIL_REQUIRES_ACCOUNT",
                                "The '" + section.getSectionCode() + "' section must contain at least one assigned account.",
                                section.getSectionCode(),
                                section.getSectionCode(),
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                }
                case SUBTOTAL, TOTAL -> {
                    if (hasAssignments) {
                        result.addError(new ValidationErrorDto(
                                "SUBTOTAL_TOTAL_CONTAINS_ACCOUNTS",
                                "The '" + section.getSectionCode() + "' section must NOT contain account assignments because it is calculated (" + type + ").",
                                section.getSectionCode(),
                                section.getSectionCode(),
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                }
                default -> {
                    // Keep behavior for any legacy/unknown enum values.
                }
            }

        }
    }
}

