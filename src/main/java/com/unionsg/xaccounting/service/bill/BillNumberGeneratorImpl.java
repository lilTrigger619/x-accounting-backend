package com.unionsg.xaccounting.service.bill;

import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.service.DocumentNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delegates to the central {@link DocumentNumberService} (Settings & Setup §14/§46) instead of
 * a standalone, unconfigurable sequence - bill numbering now shares the same admin-editable
 * prefix/padding/reset configuration as invoices, journals, and every other numbered document.
 */
@Component
@RequiredArgsConstructor
public class BillNumberGeneratorImpl implements BillNumberGenerator {

    private final DocumentNumberService documentNumberService;

    @Override
    @Transactional
    public String generateBillNumber() {
        return documentNumberService.generateNextNumber(DocumentModule.BILL);
    }
}
