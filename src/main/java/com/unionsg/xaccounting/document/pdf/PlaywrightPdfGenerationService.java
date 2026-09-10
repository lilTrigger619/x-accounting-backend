package com.unionsg.xaccounting.document.pdf;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PDF generation using a headless Chromium browser via Playwright.
 * Renders HTML the same way a real browser would (flexbox, grid, clip-path, gradients
 * all supported), so generated PDFs match the browser preview.
 *
 * <p>Playwright objects are not thread-safe and must stay on the thread that created
 * them, so the browser is created lazily on a single dedicated renderer thread and
 * every PDF request is funneled through it.</p>
 */
@Service
public class PlaywrightPdfGenerationService implements PdfGenerationService {

    private final ExecutorService rendererThread =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "pdf-renderer");
                thread.setDaemon(true);
                return thread;
            });

    private Playwright playwright;
    private Browser browser;

    @Override
    public byte[] generatePdf(String html) {
        try {
            return rendererThread.submit(() -> renderOnRendererThread(html)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PDF generation interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to generate PDF", e.getCause());
        }
    }

    private byte[] renderOnRendererThread(String html) {
        ensureBrowser();

        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();

            page.setContent(html, new Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(15000));

            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0").setLeft("0").setRight("0")));
        }
    }

    private void ensureBrowser() {
        if (browser == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
        }
    }

    @PreDestroy
    public void shutdown() {
        rendererThread.submit(() -> {
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        });
        rendererThread.shutdown();
    }
}
