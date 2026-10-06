package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionAccountRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import com.unionsg.xaccounting.service.reports.exception.TemplatePublishedDeletionException;
import com.unionsg.xaccounting.service.reports.mapper.ReportTemplateMapper;
import com.unionsg.xaccounting.service.reports.template.impl.ReportTemplateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportTemplateDeleteTest {

    @Mock private ReportTemplateRepository repository;
    @Mock private ReportTemplateSectionRepository sectionRepository;
    @Mock private ReportTemplateSectionAccountRepository sectionAccountRepository;
    @Mock private ReportTemplateMapper mapper;
    @Mock private ConfigValueValidator configValues;

    @InjectMocks
    private ReportTemplateServiceImpl service;

    @Test
    void deletingADraftRemovesItsSectionsAndAccountAssignmentsFirst() {
        ReportTemplate draft = ReportTemplate.builder().id(2L).status(ReportTemplateStatus.DRAFT).build();
        ReportTemplateSection parent = ReportTemplateSection.builder().id(10L).build();
        ReportTemplateSection child = ReportTemplateSection.builder().id(11L).parentSection(parent).build();
        List<ReportTemplateSectionAccount> assignments = List.of(ReportTemplateSectionAccount.builder().id(5L).build());
        when(repository.findById(2L)).thenReturn(Optional.of(draft));
        when(sectionRepository.findByReportTemplateId(2L)).thenReturn(List.of(parent, child));
        when(sectionAccountRepository.findByReportTemplateSectionId(10L)).thenReturn(List.of());
        when(sectionAccountRepository.findByReportTemplateSectionId(11L)).thenReturn(assignments);

        service.delete(2L);

        assertThat(child.getParentSection()).isNull();
        InOrder order = inOrder(sectionAccountRepository, sectionRepository, repository);
        order.verify(sectionAccountRepository).deleteAll(assignments);
        order.verify(sectionRepository).saveAllAndFlush(List.of(parent, child));
        order.verify(sectionRepository).deleteAll(List.of(parent, child));
        order.verify(repository).delete(draft);
    }

    @Test
    void aPublishedTemplateIsNotDeleted() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                ReportTemplate.builder().id(1L).status(ReportTemplateStatus.PUBLISHED).build()));

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(TemplatePublishedDeletionException.class);
        verify(sectionRepository, never()).deleteAll(any());
        verify(repository, never()).delete(any());
    }
}
