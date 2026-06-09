package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.entity.DocumentSequence;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.DocumentSequenceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Transactional
public class JournalNumberGeneratorImpl implements JournalNumberGenerator {

    private static final String SEQUENCE_CODE = "JOURNAL";

    private final DocumentSequenceRepository repository;

    public JournalNumberGeneratorImpl(
            DocumentSequenceRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public synchronized String generate() {

        DocumentSequence sequence = repository
                .findByCode(SEQUENCE_CODE)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Journal sequence not configured"
                        )
                );

        long nextValue = sequence.getCurrentValue() + 1;

        sequence.setCurrentValue(nextValue);

        repository.save(sequence);

        LocalDate today = LocalDate.now();

        String year = String.valueOf(today.getYear());

        String month = String.format(
                "%02d",
                today.getMonthValue()
        );

        String formattedNumber = String.format(
                "%0" + sequence.getPadding() + "d",
                nextValue
        );

        return String.format(
                "%s-%s%s-%s",
                sequence.getPrefix(),
                year,
                month,
                formattedNumber
        );
    }
}