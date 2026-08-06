package com.unionsg.xaccounting.documenttemplate.dto.request;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import lombok.Data;

/**
 * Request body for the Document Template Designer sample preview endpoint.
 *
 * <p>Represents the CURRENT UNSAVED designer state. The user can change a
 * color/layout/content setting and immediately preview it without saving first.
 * The submitted configuration is strictly read/render only — it is never persisted.</p>
 */
@Data
public class SamplePreviewRequest {

    /**
     * The layout to render. When omitted, the template's saved layout is used.
     */
    private DocumentLayout layout;

    /**
     * Unsaved design configuration (colors, fonts, logo position, etc.).
     * Fields not provided fall back to the template's saved design.
     */
    private UpdateDesignRequest design;

    /**
     * Unsaved content/visibility configuration (title, show/hide sections).
     * Fields not provided fall back to the template's saved content.
     */
    private UpdateContentRequest content;
}
