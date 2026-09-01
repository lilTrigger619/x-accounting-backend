package com.unionsg.xaccounting.entity;

import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.FileSource;
import com.unionsg.xaccounting.enums.StorageProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {
    @Id
    @GeneratedValue
//    private UUID id;
    private String id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_name, nullable = false")
    private String originalName;

    @Column(name = "file_extension", nullable = false)
    private String fileExtension;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name="file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false)
    private StorageProvider storageProvider;


    // example SYSTEM_GENERATED | USER_UPLOAD
    @Enumerated(EnumType.STRING)
    @Column(name = "file_source", nullable = false)
    private FileSource fileSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length=51)
    private EntityType entityType;

    @Column(name = "entity_id", nullable=false)
//    private UUID entityId;
    private String entityId;

    @Column(name="storage_path", nullable=false)
    private String storagePath;


    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
