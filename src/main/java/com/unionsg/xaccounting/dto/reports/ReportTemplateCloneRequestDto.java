package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Optional overrides for a cloned template. Blank fields fall back to the source template,
 * and a blank code gets the next free {@code {SOURCE}_COPY_NNN} code.
 */
public record ReportTemplateCloneRequestDto(
        @Size(max = 100)
        @Pattern(regexp = "^$|^[A-Z0-9_]+$", message = "Use uppercase letters, numbers and underscores")
        String templateCode,

        @Size(max = 200)
        String templateName,

        @Size(max = 1000)
        String description,

        @Size(max = 100)
        String category
) {
}
