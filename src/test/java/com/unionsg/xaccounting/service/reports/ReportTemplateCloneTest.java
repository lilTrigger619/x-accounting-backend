package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.dto.reports.ReportTemplateCloneRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import com.unionsg.xaccounting.service.reports.engine.FinancialReportEngine;
import com.unionsg.xaccounting.service.reports.engine.FormulaValidator;
import com.unionsg.xaccounting.service.reports.exception.TemplateCodeAlreadyExistsException;
import com.unionsg.xaccounting.service.reports.template.audit.ReportTemplateAuditService;
import com.unionsg.xaccounting.service.reports.template.lifecycle.impl.ReportTemplateLifecycleServiceImpl;
import com.unionsg.xaccounting.service.reports.template.validation.ValidationCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportTemplateCloneTest {

    @Mock private ReportTemplateRepository templateRepository;
    @Mock private ReportTemplateSectionRepository sectionRepository;
    @Mock private ReportTemplateSectionAccountRepository sectionAccountRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private FinancialReportEngine financialReportEngine;
    @Mock private FormulaValidator formulaValidator;
    @Mock private ConfigValueValidator configValues;
    @Mock private ValidationCoordinator validationCoordinator;
    @Mock private ReportTemplateAuditService auditService;

    @InjectMocks
    private ReportTemplateLifecycleServiceImpl service;

    private final ReportTemplate source = ReportTemplate.builder()
            .id(7L)
            .templateCode("PL_STANDARD")
            .templateName("Standard P&L")
            .description("Seeded")
            .category("FINANCIAL_REPORTS")
            .status(ReportTemplateStatus.PUBLISHED)
            .version(3)
            .isSystemTemplate(true)
            .build();

    @BeforeEach
    void setUp() {
        when(templateRepository.findById(7L)).thenReturn(Optional.of(source));
        lenient().when(templateRepository.save(any(ReportTemplate.class))).thenAnswer(inv -> {
            ReportTemplate t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });
        lenient().when(sectionRepository.findByReportTemplateId(7L)).thenReturn(List.of());
    }

    @Test
    void cloneUsesTheRequestedCodeNameDescriptionAndCategory() {
        when(configValues.validate("report-categories", "CUSTOM", "FINANCIAL_REPORTS", "Category")).thenReturn("CUSTOM");

        ReportTemplateDto clone = service.clone(7L,
                new ReportTemplateCloneRequestDto("PL_BRANCH", "Branch P&L", "Per branch", "CUSTOM"), "admin");

        ArgumentCaptor<ReportTemplate> saved = ArgumentCaptor.forClass(ReportTemplate.class);
        verify(templateRepository).save(saved.capture());
        assertThat(saved.getValue().getTemplateCode()).isEqualTo("PL_BRANCH");
        assertThat(saved.getValue().getTemplateName()).isEqualTo("Branch P&L");
        assertThat(saved.getValue().getDescription()).isEqualTo("Per branch");
        assertThat(saved.getValue().getCategory()).isEqualTo("CUSTOM");
        assertThat(saved.getValue().getStatus()).isEqualTo(ReportTemplateStatus.DRAFT);
        assertThat(saved.getValue().isSystemTemplate()).isFalse();
        assertThat(clone.id()).isEqualTo(99L);
    }

    @Test
    void cloneWithoutOverridesKeepsTheSourceDetailsAndGeneratesACopyCode() {
        when(templateRepository.findAll()).thenReturn(List.of(source));

        service.clone(7L, null, "admin");

        ArgumentCaptor<ReportTemplate> saved = ArgumentCaptor.forClass(ReportTemplate.class);
        verify(templateRepository).save(saved.capture());
        assertThat(saved.getValue().getTemplateCode()).isEqualTo("PL_STANDARD_COPY_001");
        assertThat(saved.getValue().getTemplateName()).isEqualTo("Standard P&L");
        assertThat(saved.getValue().getCategory()).isEqualTo("FINANCIAL_REPORTS");
    }

    @Test
    void cloneRejectsACodeThatIsAlreadyTaken() {
        when(templateRepository.existsByTemplateCode("PL_STANDARD")).thenReturn(true);

        assertThatThrownBy(() -> service.clone(7L,
                new ReportTemplateCloneRequestDto("PL_STANDARD", "Copy", null, null), "admin"))
                .isInstanceOf(TemplateCodeAlreadyExistsException.class);
        verify(templateRepository, never()).save(any());
    }
}
