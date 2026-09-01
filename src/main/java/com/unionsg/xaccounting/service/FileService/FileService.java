package com.unionsg.xaccounting.service.FileService;

import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileService {
    List<FileResponseDto> uploadFile(
            MultipartFile[] files,
            FileUploadRequestDto request
    );

    Page<FileResponseDto> getFiles(
            EntityType entityType,
            UUID entityId,
            String mimeType,
            Pageable pageable
    );

    FileResponseDto getFile(String id);

    void deleteFile(String id);
}
