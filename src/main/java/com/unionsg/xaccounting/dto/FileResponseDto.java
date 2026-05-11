package com.unionsg.xaccounting.dto;

import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.FileSource;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileResponseDto {
    private UUID id;

    private String fileName;

    private String originalName;

    private String fileExtension;

    private String mimeType;

    private Long fileSize;

    private FileSource fileSource;

    private EntityType entityType;

    private String description;

    private LocalDateTime createdAt;

//    private UUID entityId;
    private String entityId;

    private String storagePath;
}
