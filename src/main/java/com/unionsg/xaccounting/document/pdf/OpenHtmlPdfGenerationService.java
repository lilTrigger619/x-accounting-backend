package com.unionsg.xaccounting.document.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * PDF generation using OpenHTMLToPDF library.
 * Converts HTML string to PDF byte array.
 */
@Service
public class OpenHtmlPdfGenerationService implements PdfGenerationService {

    @Override
    public byte[] generatePdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}

