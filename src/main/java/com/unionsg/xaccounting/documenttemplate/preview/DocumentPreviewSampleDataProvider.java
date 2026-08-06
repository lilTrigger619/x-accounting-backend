package com.unionsg.xaccounting.documenttemplate.preview;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;

/**
 * Strategy for building in-memory sample document data used by the
 * Document Template Designer sample preview flow.
 *
 * <p>Implementations MUST return the same document model used by the real
 * rendering pipeline (e.g. {@link com.unionsg.xaccounting.entity.invoice.Invoice}),
 * so the sample preview exercises the exact same Thymeleaf template structure.</p>
 *
 * <p>The provider is extensible: later implementations can be added for
 * CreditNote, Quote, PaymentReceipt, CustomerStatement, etc. without changing
 * the controller architecture.</p>
 */
public interface DocumentPreviewSampleDataProvider {

    /**
     * The document type this provider produces sample data for.
     */
    DocumentType getDocumentType();

    /**
     * Builds a fresh in-memory sample document model.
     *
     * <p>This method must NOT persist anything to the database and must NOT
     * consume any document number sequences or mutate account balances. The
     * returned object exists only for the duration of the HTTP request.</p>
     */
    Object buildSampleData();
}
