package com.unionsg.xaccounting.service.FileService;


import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String upload(MultipartFile file, String fileName);

    Resource download(String filePath);

    void delete(String filePath);
}
