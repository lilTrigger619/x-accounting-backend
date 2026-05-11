package com.unionsg.xaccounting.service.FileService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file, String fileName) {

        try {

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return filePath.toString();

        } catch (IOException ex) {

            throw new RuntimeException("Could not store file", ex);
        }
    }


    @Override
    public Resource download(String filePath) {

        try {

            Path path = Paths.get(filePath).normalize();

            Resource resource = new UrlResource(path.toUri());

            if (resource.exists()) {
                return resource;
            }

            throw new RuntimeException("File not found");

        } catch (MalformedURLException ex) {

            throw new RuntimeException("File not found", ex);
        }
    }


    @Override
    public void delete(String filePath) {

        try {

            Path path = Paths.get(filePath).normalize();

            Files.deleteIfExists(path);

        } catch (IOException ex) {

            throw new RuntimeException("Could not delete file", ex);
        }
    }

}