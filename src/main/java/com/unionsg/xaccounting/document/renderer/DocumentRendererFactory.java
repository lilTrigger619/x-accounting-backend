package com.unionsg.xaccounting.document.renderer;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory that resolves a DocumentRenderer based on DocumentLayout.
 * Uses a registry pattern — no if/else statements.
 */
@Component
@RequiredArgsConstructor
public class DocumentRendererFactory {

    private final List<DocumentRenderer> renderers;

    private final Map<DocumentLayout, DocumentRenderer> registry = new EnumMap<>(DocumentLayout.class);

    @PostConstruct
    public void init() {
        for (DocumentRenderer renderer : renderers) {
            register(renderer);
        }
    }

    private void register(DocumentRenderer renderer) {
        String path = renderer.getTemplatePath();

        if (path.contains("classic")) {
            registry.put(DocumentLayout.CLASSIC, renderer);
        } else if (path.contains("modern")) {
            registry.put(DocumentLayout.MODERN, renderer);
        } else if (path.contains("professional")) {
            registry.put(DocumentLayout.PROFESSIONAL, renderer);
        }
    }

    public DocumentRenderer getRenderer(DocumentLayout layout) {
        DocumentRenderer renderer = registry.get(layout);
        if (renderer == null) {
            throw new IllegalArgumentException("No renderer found for layout: " + layout);
        }
        return renderer;
    }
}

