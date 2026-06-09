package com.unionsg.xaccounting.service.journal;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
public class JournalPostingServiceImpl implements JournalPostingService {

    @Override
    public void validateJournal(JournalEntry journal) {

        validateLines(journal);

        validateBalanced(journal);

        validatePostingPeriod(journal);
    }

    @Override
    public void validateBalanced(JournalEntry journal) {

        if (journal.getTotalDebit()
                .compareTo(journal.getTotalCredit()) != 0) {

            throw new BadRequestException(
                    "Journal is not balanced"
            );
        }
    }

    @Override
    public void validateLines(JournalEntry journal) {

        if (journal.getLines() == null
                || journal.getLines().size() < 2) {

            throw new BadRequestException(
                    "Journal must contain at least 2 lines"
            );
        }

        for (JournalLine line : journal.getLines()) {

            BigDecimal debit = line.getDebitAmount();

            BigDecimal credit = line.getCreditAmount();

            boolean hasDebit =
                    debit != null
                            && debit.compareTo(BigDecimal.ZERO) > 0;

            boolean hasCredit =
                    credit != null
                            && credit.compareTo(BigDecimal.ZERO) > 0;

            if (hasDebit == hasCredit) {

                throw new BadRequestException(
                        "Each line must contain either debit or credit"
                );
            }
        }
    }

    @Override
    public void validatePostingPeriod(JournalEntry journal) {

        // future implementation
        // validate accounting periods here
    }

    @Override
    public void post(JournalEntry journal) {

        if (journal.getStatus() != JournalStatus.DRAFT) {

            throw new BadRequestException(
                    "Only draft journals can be posted"
            );
        }

        validateJournal(journal);

        journal.setStatus(JournalStatus.POSTED);

        journal.setPostingDate(LocalDate.now());

        journal.setPostedAt(LocalDateTime.now());
    }
}