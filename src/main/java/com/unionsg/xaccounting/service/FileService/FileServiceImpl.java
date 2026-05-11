package com.unionsg.xaccounting.service.FileService;

import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.entity.FileEntity;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.FileSource;
import com.unionsg.xaccounting.enums.StorageProvider;
import com.unionsg.xaccounting.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @Override
    public List<FileResponseDto> uploadFile(
            MultipartFile[] files,
            FileUploadRequestDto request
    ) {

        List<FileResponseDto> responses = new ArrayList<>();

        for (MultipartFile file : files){

            validateFile(file);

            String fileName = generateFileName(file);

            String storagePath = fileStorageService.upload(file, fileName);

            FileEntity entity = FileEntity.builder()
                    .fileName(fileName)
                    .originalName(file.getOriginalFilename())
                    .fileExtension(getExtension(file))
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .storageProvider(StorageProvider.LOCAL)
                    .storagePath(storagePath)
                    .fileSource(FileSource.USER_UPLOAD)
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .description(request.getDescription())
                    .isDeleted(false)
                    .build();

            fileRepository.save(entity);
            responses.add(mapToDto(entity));
        }

        return responses;
    }

    @Override
    public Page<FileResponseDto> getFiles(
            EntityType entityType,
            UUID entityId,
            Pageable pageable
    ) {

        var data =  fileRepository
                .findByEntityTypeAndEntityIdAndIsDeletedFalse(
                        entityType,
                        entityId,
                        pageable
                )
                .map(this::mapToDto);
        return data;
    }

    @Override
    public FileResponseDto getFile(UUID id) {

        FileEntity entity = fileRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        return mapToDto(entity);
    }

    @Override
    public void deleteFile(UUID id) {

        FileEntity entity = fileRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        entity.setIsDeleted(true);

        fileRepository.save(entity);

    }

    private FileResponseDto mapToDto(FileEntity entity) {

        return FileResponseDto.builder()
                .id(UUID.fromString(entity.getId()))
                .fileName(entity.getFileName())
                .originalName(entity.getOriginalName())
                .fileExtension(entity.getFileExtension())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .fileSource(entity.getFileSource())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // service utitlities
    private String  generateFileName(MultipartFile file){
        String extension = getExtension(file);

        return UUID.randomUUID() + "." + extension;
    };

    private String getExtension(MultipartFile file){
        String fileName = file.getOriginalFilename();

        return fileName.substring(fileName.lastIndexOf(".")+1);
    }

    private void validateFile(MultipartFile file){
        if (file.isEmpty()){
            throw new RuntimeException("File is empty");
        }

        var cleanFilename  = StringUtils.cleanPath(file.getOriginalFilename());
        if (cleanFilename.contains("..")){
            throw new RuntimeException("Invalid file name");
        }

        long maxSize = 10 * 1024 * 1024;

        if (file.getSize() > maxSize){
            throw new RuntimeException("File size exceeds limit");
        }
    }
}