package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.UpdateJournalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JournalService {
    JournalResponse create(CreateJournalRequest request);

    JournalResponse update(Long id, UpdateJournalRequest request);

    JournalResponse getById(Long id);

    JournalResponse getByJournalNumber(String journalNumber);

    Page<JournalResponse> getAll(Pageable pageable);

    void deleteDraft(Long id);

    JournalResponse post(Long id);

    JournalResponse reverse(Long id, String reason);

}
