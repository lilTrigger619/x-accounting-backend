package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.service.DocumentNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delegates to the central {@link DocumentNumberService} (Settings & Setup §14/§46) instead of
 * a standalone, unconfigurable sequence. Customer receipts are tracked under
 * {@link DocumentModule#PAYMENT}.
 */
@Component
@RequiredArgsConstructor
public class ReceiptNumberGeneratorImpl implements ReceiptNumberGenerator {

    private final DocumentNumberService documentNumberService;

    @Override
    @Transactional
    public String generateReceiptNumber() {
        return documentNumberService.generateNextNumber(DocumentModule.PAYMENT);
    }
}
