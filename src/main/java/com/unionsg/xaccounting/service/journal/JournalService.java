package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.UpdateJournalRequest;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface JournalService {
    JournalResponse create(CreateJournalRequest request);

    JournalResponse update(Long id, UpdateJournalRequest request);

    JournalResponse getById(Long id);

    JournalResponse getByJournalNumber(String journalNumber);

    Page<JournalResponse> getAll(
            String search,
            JournalStatus status,
            JournalType journalType,
            LocalDate fromDate,
            LocalDate toDate,
            String sourceModule,
            Pageable pageable
    );

    void deleteDraft(Long id);

    JournalResponse post(Long id);

    JournalResponse reverse(Long id, String reason);

}
