package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.MapperLayer.JournalMapper;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.ReverseJournalRequest;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceImplTest {

    @Mock private JournalEntryRepository journalRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private JournalMapper journalMapper;
    @Mock private JournalPostingService postingService;
    @Mock private JournalNumberGenerator numberGenerator;
    @Mock private DocumentNumberService documentNumberService;
    @Mock private PeriodLockGuard periodLockGuard;
    @Mock private ConfigValueValidator configValues;

    @InjectMocks
    private JournalServiceImpl service;

    private static CreateJournalRequest manualRequest(JournalType type, String currency) {
        CreateJournalLineRequest line = CreateJournalLineRequest.builder()
                .accountId(1000L).debitAmount(new BigDecimal("50")).creditAmount(BigDecimal.ZERO).build();
        return CreateJournalRequest.builder()
                .journalDate(LocalDate.of(2026, 10, 1))
                .journalType(type)
                .currencyCode(currency)
                .lines(List.of(line))
                .build();
    }

    private static JournalEntry postedJournal() {
        JournalEntry journal = new JournalEntry();
        journal.setId(7L);
        journal.setJournalNumber("JV-0007");
        journal.setJournalType(JournalType.GENERAL);
        journal.setStatus(JournalStatus.POSTED);
        journal.setJournalDate(LocalDate.of(2026, 10, 1));
        return journal;
    }

    @Test
    void manualJournalRejectsModuleOwnedType() {
        assertThatThrownBy(() -> service.createManualJournal(manualRequest(JournalType.PREPAYMENT, "USD")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("can't be entered by hand");
        verify(journalRepository, never()).save(any());
    }

    @Test
    void manualJournalRejectsUnconfiguredCurrency() {
        when(configValues.require("currencies", "GHC", null, "Currency"))
                .thenThrow(new BusinessException("\"GHC\" is not a valid currency. Pick one from the list or add it first."));
        assertThatThrownBy(() -> service.createManualJournal(manualRequest(JournalType.GENERAL, "GHC")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a valid currency");
        verify(journalRepository, never()).save(any());
    }

    @Test
    void manualTypesExcludeModuleOwnedTypes() {
        assertThat(JournalType.GENERAL.isManualEntry()).isTrue();
        assertThat(JournalType.ADJUSTMENT.isManualEntry()).isTrue();
        assertThat(JournalType.OPENING_BALANCE.isManualEntry()).isFalse();
        assertThat(JournalType.REVERSING.isManualEntry()).isFalse();
        assertThat(JournalType.LOAN.isManualEntry()).isFalse();
    }

    @Test
    void reversalRejectsDateBeforeOriginal() {
        when(journalRepository.findById(7L)).thenReturn(Optional.of(postedJournal()));
        ReverseJournalRequest request = ReverseJournalRequest.builder()
                .reason("Wrong account").reverseDate(LocalDate.of(2026, 9, 30)).build();
        assertThatThrownBy(() -> service.reverse(7L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("can't be before");
        verify(journalRepository, never()).save(any());
    }

    @Test
    void reversalUsesRequestedDateAndReference() {
        JournalEntry original = postedJournal();
        when(journalRepository.findById(7L)).thenReturn(Optional.of(original));
        when(documentNumberService.generateNextNumber(DocumentModule.JOURNAL)).thenReturn("JV-0008");
        when(journalMapper.toResponse(any())).thenReturn(new JournalResponse());

        service.reverse(7L, ReverseJournalRequest.builder()
                .reason("Wrong account").reverseDate(LocalDate.of(2026, 10, 3)).reference("FIX-1").build());

        ArgumentCaptor<JournalEntry> saved = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalRepository, atLeastOnce()).save(saved.capture());
        JournalEntry reversal = saved.getAllValues().get(0);
        assertThat(reversal.getJournalDate()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(reversal.getReference()).isEqualTo("FIX-1");
        assertThat(reversal.getDescription()).isEqualTo("Wrong account");
        assertThat(reversal.getReversalOfJournalId()).isEqualTo(7L);
        assertThat(original.getStatus()).isEqualTo(JournalStatus.REVERSED);
        verify(periodLockGuard).assertPostable(LocalDate.of(2026, 10, 3));
    }

    @Test
    void reversalDefaultsToTodayAndRevReference() {
        when(journalRepository.findById(7L)).thenReturn(Optional.of(postedJournal()));
        when(documentNumberService.generateNextNumber(DocumentModule.JOURNAL)).thenReturn("JV-0008");
        when(journalMapper.toResponse(any())).thenReturn(new JournalResponse());

        service.reverse(7L, "Duplicate entry");

        ArgumentCaptor<JournalEntry> saved = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalRepository, atLeastOnce()).save(saved.capture());
        JournalEntry reversal = saved.getAllValues().get(0);
        assertThat(reversal.getJournalDate()).isEqualTo(LocalDate.now());
        assertThat(reversal.getReference()).isEqualTo("REV-JV-0007");
    }
}
