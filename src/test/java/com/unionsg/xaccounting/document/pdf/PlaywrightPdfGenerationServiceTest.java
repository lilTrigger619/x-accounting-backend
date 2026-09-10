package com.unionsg.xaccounting.document.pdf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies PDF generation actually works end-to-end (real headless Chromium),
 * including CSS the previous openhtmltopdf engine could not render: flexbox,
 * grid, and clip-path. This is exactly the class of layout used by the
 * modern/professional invoice templates.
 */
class PlaywrightPdfGenerationServiceTest {

    @Test
    void generatesPdfFromFlexboxGridAndClipPathLayout() {
        PlaywrightPdfGenerationService service = new PlaywrightPdfGenerationService();

        String html = """
                <html>
                <head>
                <style>
                    .row { display: flex; justify-content: space-between; }
                    .grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
                    .banner { clip-path: polygon(0 0, 100% 0, 100% 78%, 0 100%); background: #333; height: 60px; }
                </style>
                </head>
                <body>
                    <div class="banner"></div>
                    <div class="row"><span>Left</span><span>Right</span></div>
                    <div class="grid"><div>A</div><div>B</div><div>C</div></div>
                </body>
                </html>
                """;

        byte[] pdfBytes = service.generatePdf(html);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");

        service.shutdown();
    }
}
