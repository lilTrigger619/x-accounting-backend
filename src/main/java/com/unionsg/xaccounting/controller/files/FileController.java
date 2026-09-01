package com.unionsg.xaccounting.controller.files;


import ch.qos.logback.core.net.SyslogOutputStream;
import com.google.gson.Gson;
import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.FileService.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.json.GsonDecoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {


    private final FileService fileService;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileResponseDto>> uploadFile(
            @RequestPart("files") MultipartFile[] files,
//            @RequestPart("data") @Valid FileUploadRequestDto request
            @RequestPart("data") @Valid String request
    ) {



        Gson gson = new Gson();
        System.out.println("Before json was parsed ");
        FileUploadRequestDto fileUploadRequestDto  = gson.fromJson(request, FileUploadRequestDto.class);
        System.out.println("Before printing the content of the dto");
        System.out.println(fileUploadRequestDto.toString());
        List<FileResponseDto> response = fileService.uploadFile(files, fileUploadRequestDto);

       return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<Page<FileResponseDto>> getFiles(
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String mimeType,
            Pageable pageable
    ) {

        Page<FileResponseDto> response =
                fileService.getFiles(entityType, entityId, mimeType, pageable);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<FileResponseDto> getFile(
            @PathVariable String id
    ) {

        FileResponseDto response = fileService.getFile(id);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String id
    ) {

        fileService.deleteFile(id);

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String id
    ) {

        FileResponseDto file = fileService.getFile(id);

        Resource resource =
                fileStorageService.download(file.getStoragePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                file.getOriginalName() + "\""
                )
                .body(resource);
    }

}