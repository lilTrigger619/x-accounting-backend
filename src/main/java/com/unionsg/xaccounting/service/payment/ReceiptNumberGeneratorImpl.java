package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.entity.DocumentSequence;
import com.unionsg.xaccounting.repository.DocumentSequenceRepository;
import com.unionsg.xaccounting.utils.PaymentConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class ReceiptNumberGeneratorImpl implements ReceiptNumberGenerator {

    private final DocumentSequenceRepository documentSequenceRepository;

    @Override
    @Transactional
    public String generateReceiptNumber() {
        DocumentSequence sequence = documentSequenceRepository.findByCode(PaymentConstants.SEQUENCE_CODE)
                .orElseGet(this::createDefaultSequence);

        long nextValue = sequence.getCurrentValue() + 1;
        sequence.setCurrentValue(nextValue);
        documentSequenceRepository.save(sequence);

        return buildReceiptNumber(nextValue);
    }

    private String buildReceiptNumber(long number) {
        YearMonth yearMonth = YearMonth.now();
        String yearMonthStr = yearMonth.getYear()
                + String.format("%02d", yearMonth.getMonthValue());

        return PaymentConstants.RECEIPT_PREFIX
                + PaymentConstants.RECEIPT_SEPARATOR
                + yearMonthStr
                + PaymentConstants.RECEIPT_SEPARATOR
                + String.format("%06d", number);
    }

    private DocumentSequence createDefaultSequence() {
        DocumentSequence sequence = new DocumentSequence();
        sequence.setCode(PaymentConstants.SEQUENCE_CODE);
        sequence.setPrefix(PaymentConstants.RECEIPT_PREFIX);
        sequence.setCurrentValue(0L);
        sequence.setPadding(6);
        return documentSequenceRepository.save(sequence);
    }
}
