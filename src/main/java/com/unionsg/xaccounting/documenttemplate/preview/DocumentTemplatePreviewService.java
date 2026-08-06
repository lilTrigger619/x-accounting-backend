package com.unionsg.xaccounting.documenttemplate.preview;

import com.unionsg.xaccounting.documenttemplate.dto.request.SamplePreviewRequest;

/**
 * Service for generating a sample preview of a Document Template for the
 * Document Template Designer.
 *
 * <p>The preview renders a fabricated, in-memory invoice using the template's
 * saved configuration overlaid with the current unsaved designer state. Nothing
 * is persisted to the database and no document number sequences or account
 * balances are mutated.</p>
 */
public interface DocumentTemplatePreviewService {

    /**
     * Renders the fully rendered invoice HTML for the given template and the
     * current unsaved designer configuration.
     *
     * @param templateId the document template being customized
     * @param request    the current unsaved designer state (layout/design/content)
     * @return the rendered invoice HTML
     */
    String samplePreview(Long templateId, SamplePreviewRequest request);
}
