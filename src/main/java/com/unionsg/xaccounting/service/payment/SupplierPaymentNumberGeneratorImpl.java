package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.service.DocumentNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delegates to the central {@link DocumentNumberService} (Settings & Setup §14/§46) instead of
 * a standalone, unconfigurable sequence.
 */
@Component
@RequiredArgsConstructor
public class SupplierPaymentNumberGeneratorImpl implements SupplierPaymentNumberGenerator {

    private final DocumentNumberService documentNumberService;

    @Override
    @Transactional
    public String generatePaymentNumber() {
        return documentNumberService.generateNextNumber(DocumentModule.SUPPLIER_PAYMENT);
    }
}
