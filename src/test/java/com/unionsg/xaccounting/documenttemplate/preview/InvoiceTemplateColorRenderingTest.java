package com.unionsg.xaccounting.documenttemplate.preview;

import com.unionsg.xaccounting.document.context.CompanyInfo;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateContent;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateDesign;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the real invoice templates through the actual Thymeleaf engine (no mocked
 * renderer) to guard against regressions in how design colors/fonts are inlined into
 * the {@code <style th:inline="css">} block. Thymeleaf's CSS-escaped inlining
 * ({@code [[...]]}) backslash-escapes characters like '#', '(', ')', and ',', which
 * silently breaks hex and rgb()/rgba() color values while leaving plain named colors
 * (e.g. "red") untouched. The templates must use unescaped inlining ({@code [(...)]})
 * for these values instead.
 */
class InvoiceTemplateColorRenderingTest {

    private final SpringTemplateEngine engine = buildEngine();
    private final InvoiceSampleDataProvider sampleDataProvider = new InvoiceSampleDataProvider();

    private static SpringTemplateEngine buildEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private Context buildContext(DocumentTemplateDesign design) {
        Context ctx = new Context(Locale.US);
        ctx.setVariable("design", design);
        ctx.setVariable("content", content());
        ctx.setVariable("company", company());
        Object invoice = sampleDataProvider.buildSampleData();
        ctx.setVariable("invoice", invoice);
        ctx.setVariable("document", invoice);
        ctx.setVariable("baseUrl", "http://localhost:8080");
        return ctx;
    }

    private DocumentTemplateContent content() {
        DocumentTemplateContent content = new DocumentTemplateContent();
        content.setFormTitle("Invoice");
        content.setShowCompanyName(true);
        content.setShowBillingAddress(true);
        content.setShowDueDate(true);
        content.setShowPaymentMethod(true);
        return content;
    }

    private CompanyInfo company() {
        return CompanyInfo.builder()
                .name("Acme Ltd")
                .phone("+233 24 000 0000")
                .email("billing@acme.example")
                .website("acme.example")
                .addressLine1("1 Main Street")
                .city("Accra")
                .country("Ghana")
                .build();
    }

    private CompanyInfo companyWithLogo() {
        CompanyInfo company = company();
        company.setLogoUrl("http://localhost:8080/uploads/logo.png");
        return company;
    }

    private Context buildContext(DocumentTemplateDesign design, boolean withLogo) {
        Context ctx = buildContext(design);
        if (withLogo) {
            ctx.setVariable("company", companyWithLogo());
        }
        return ctx;
    }

    private DocumentTemplateDesign hexAndRgbDesign() {
        DocumentTemplateDesign design = new DocumentTemplateDesign();
        design.setPrimaryColor("#2563EB");
        design.setSecondaryColor("rgb(100, 116, 139)");
        design.setFontFamily("Arial, sans-serif");
        design.setFontSize(12);
        design.setFontColor("#111827");
        return design;
    }

    @Test
    void classicTemplateRendersHexAndRgbColorsUnescaped() {
        String html = engine.process("documents/invoice/classic", buildContext(hexAndRgbDesign()));

        assertThat(html).contains("#2563EB");
        assertThat(html).contains("rgb(100, 116, 139)");
        assertThat(html).contains("#111827");
        assertThat(html).doesNotContain("\\#");
        assertThat(html).doesNotContain("\\(");
    }

    @Test
    void modernTemplateRendersHexAndRgbColorsUnescaped() {
        String html = engine.process("documents/invoice/modern", buildContext(hexAndRgbDesign()));

        assertThat(html).contains("#2563EB");
        assertThat(html).contains("rgb(100, 116, 139)");
        assertThat(html).contains("#111827");
        assertThat(html).doesNotContain("\\#");
        assertThat(html).doesNotContain("\\(");
    }

    @Test
    void professionalTemplateRendersHexAndRgbColorsUnescaped() {
        String html = engine.process("documents/invoice/professional", buildContext(hexAndRgbDesign()));

        assertThat(html).contains("#2563EB");
        assertThat(html).contains("rgb(100, 116, 139)");
        assertThat(html).contains("#111827");
        assertThat(html).doesNotContain("\\#");
        assertThat(html).doesNotContain("\\(");
    }

    @Test
    void classicTemplateExposesLogoIdOnFallbackAndActualLogo() {
        String withoutLogo = engine.process("documents/invoice/classic", buildContext(hexAndRgbDesign(), false));
        String withLogo = engine.process("documents/invoice/classic", buildContext(hexAndRgbDesign(), true));

        assertThat(withoutLogo).contains("id=\"document-logo\"");
        assertThat(withLogo).contains("id=\"document-logo\"");
    }

    @Test
    void modernTemplateExposesLogoIdOnFallbackAndActualLogo() {
        String withoutLogo = engine.process("documents/invoice/modern", buildContext(hexAndRgbDesign(), false));
        String withLogo = engine.process("documents/invoice/modern", buildContext(hexAndRgbDesign(), true));

        assertThat(withoutLogo).contains("id=\"document-logo\"");
        assertThat(withLogo).contains("id=\"document-logo\"");
    }

    @Test
    void professionalTemplateExposesLogoIdOnFallbackAndActualLogo() {
        String withoutLogo = engine.process("documents/invoice/professional", buildContext(hexAndRgbDesign(), false));
        String withLogo = engine.process("documents/invoice/professional", buildContext(hexAndRgbDesign(), true));

        assertThat(withoutLogo).contains("id=\"document-logo\"");
        assertThat(withLogo).contains("id=\"document-logo\"");
    }
}
