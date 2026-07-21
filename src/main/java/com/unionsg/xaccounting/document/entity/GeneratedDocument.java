package com.unionsg.xaccounting.document.entity;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks generated document files.
 * Stores a reference to the file via fileId (from the File Upload module).
 * Does NOT store the actual file content — that belongs in the File Upload module.
 */
@Entity
@Table(name = "generated_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "template_version", nullable = false)
    private Long templateVersion;

    @Column(name = "file_id", nullable = false, length = 100)
    private String fileId;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}

