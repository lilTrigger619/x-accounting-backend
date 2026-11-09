package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.dto.branch.BranchAccountingSettingsRequest;
import com.unionsg.xaccounting.dto.branch.BranchAccountingSettingsResponse;
import com.unionsg.xaccounting.entity.branch.BranchAccountingSettings;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.branch.BranchAccountingSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BranchAccountingSettingsServiceTest {

    @Mock BranchAccountingSettingsRepository repository;
    @Mock AccountRepository accountRepository;
    @InjectMocks BranchAccountingSettingsService service;

    private BranchAccountingSettings row;

    @BeforeEach
    void setUp() {
        row = new BranchAccountingSettings();
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(row));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void lineEntryDefaultsToGrid() {
        assertThat(service.get().getJournalLineEntryMode()).isEqualTo("GRID");
    }

    @Test
    void switchesToModalAndBack() {
        BranchAccountingSettingsRequest request = new BranchAccountingSettingsRequest();
        request.setJournalLineEntryMode(" modal ");
        BranchAccountingSettingsResponse saved = service.update(request);
        assertThat(saved.getJournalLineEntryMode()).isEqualTo("MODAL");
        assertThat(row.getJournalLineEntryMode()).isEqualTo("MODAL");

        request.setJournalLineEntryMode("GRID");
        assertThat(service.update(request).getJournalLineEntryMode()).isEqualTo("GRID");
    }

    @Test
    void leavesTheModeAloneWhenNotSent() {
        row.setJournalLineEntryMode("MODAL");
        assertThat(service.update(new BranchAccountingSettingsRequest()).getJournalLineEntryMode()).isEqualTo("MODAL");
    }

    @Test
    void refusesAnUnknownMode() {
        BranchAccountingSettingsRequest request = new BranchAccountingSettingsRequest();
        request.setJournalLineEntryMode("SPREADSHEET");
        assertThatThrownBy(() -> service.update(request)).isInstanceOf(BusinessException.class).hasMessageContaining("GRID");
    }
}
