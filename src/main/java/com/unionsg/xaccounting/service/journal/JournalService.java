package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.ReverseJournalRequest;
import com.unionsg.xaccounting.dto.journal.UpdateJournalRequest;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface JournalService {
    JournalResponse create(CreateJournalRequest request);

    /**
     * Same as {@link #create(CreateJournalRequest)}, but for the manual Journal Entry screen
     * only: rejects any line that targets a control account (Settings & Setup §8), since those
     * accounts must only ever be touched by the subledger posting service that reconciles them
     * (e.g. Accounts Receivable via an invoice/payment, Salary Payable via a payroll run).
     */
    JournalResponse createManualJournal(CreateJournalRequest request);

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

    /** Reverses a posted journal today with the default "REV-" reference. */
    JournalResponse reverse(Long id, String reason);

    /** Reverses a posted journal on the request's date (today when omitted) and reference. */
    JournalResponse reverse(Long id, ReverseJournalRequest request);

}
