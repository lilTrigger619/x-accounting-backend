package com.unionsg.xaccounting.document.renderer;

/**
 * Strategy interface for document rendering.
 * Each implementation selects the Thymeleaf template path for a specific layout.
 * Renderers must NOT contain HTML generation logic — that belongs in the Thymeleaf template.
 */
public interface DocumentRenderer {

    /**
     * Returns the Thymeleaf template path for this renderer.
     * Example: "documents/invoice/classic"
     */
    String getTemplatePath();
}

