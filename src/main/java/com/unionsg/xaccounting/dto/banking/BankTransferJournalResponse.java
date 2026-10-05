package com.unionsg.xaccounting.dto.banking;

import com.unionsg.xaccounting.dto.journal.JournalResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferJournalResponse {
    private JournalResponse journal;
    private JournalResponse reversalJournal;
}
