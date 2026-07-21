package com.unionsg.xaccounting.document.pdf;

/**
 * Service interface for PDF generation.
 * Implementations convert HTML strings to PDF byte arrays.
 */
public interface PdfGenerationService {

    byte[] generatePdf(String html);
}

