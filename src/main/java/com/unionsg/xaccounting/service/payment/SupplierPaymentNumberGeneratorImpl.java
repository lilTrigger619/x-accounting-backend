package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.entity.DocumentSequence;
import com.unionsg.xaccounting.repository.DocumentSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class SupplierPaymentNumberGeneratorImpl implements SupplierPaymentNumberGenerator {

    private static final String SEQUENCE_CODE = "SUPPLIER_PAYMENT";
    private static final String PREFIX = "SPMT";
    private static final String SEPARATOR = "-";

    private final DocumentSequenceRepository documentSequenceRepository;

    @Override
    @Transactional
    public String generatePaymentNumber() {
        DocumentSequence sequence = documentSequenceRepository.findByCode(SEQUENCE_CODE)
                .orElseGet(this::createDefaultSequence);

        long nextValue = sequence.getCurrentValue() + 1;
        sequence.setCurrentValue(nextValue);
        documentSequenceRepository.save(sequence);

        return buildPaymentNumber(nextValue);
    }

    private String buildPaymentNumber(long number) {
        YearMonth yearMonth = YearMonth.now();
        String yearMonthStr = yearMonth.getYear()
                + String.format("%02d", yearMonth.getMonthValue());

        return PREFIX + SEPARATOR + yearMonthStr + SEPARATOR + String.format("%06d", number);
    }

    private DocumentSequence createDefaultSequence() {
        DocumentSequence sequence = new DocumentSequence();
        sequence.setCode(SEQUENCE_CODE);
        sequence.setPrefix(PREFIX);
        sequence.setCurrentValue(0L);
        sequence.setPadding(6);
        return documentSequenceRepository.save(sequence);
    }
}
