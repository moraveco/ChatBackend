package com.moraveco.springboot.storage.service;

import com.moraveco.springboot.util.ImageUtils;
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
public class FileStorageService {

    private static final String SHARED_ROOT = "/fileserver";
    private static final String IMAGE_DIR = SHARED_ROOT + "/uploads/images/";
    private static final String PROFILE_DIR = SHARED_ROOT + "/uploads/profiles/";

    public FileStorageService() {
        try {
            Files.createDirectories(Paths.get(IMAGE_DIR));
            Files.createDirectories(Paths.get(PROFILE_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public String saveImage(MultipartFile file) throws IOException {
        if (!ImageUtils.isImageFile(file)) {
            throw new IllegalArgumentException("Invalid image format");
        }
        return saveFile(file, IMAGE_DIR);
    }

    public String saveProfileImage(MultipartFile file) throws IOException {
        if (!ImageUtils.isImageFile(file)) {
            throw new IllegalArgumentException("Invalid image format");
        }
        return saveFile(file, PROFILE_DIR);
    }

    private String saveFile(MultipartFile file, String dir) throws IOException {
        String newFilename = ImageUtils.generateUniqueFilename(file.getOriginalFilename());
        Path targetPath = Paths.get(dir).resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return newFilename;
    }

    public Resource load(String filename, boolean isProfile) {
        try {
            Path root = Paths.get(isProfile ? PROFILE_DIR : IMAGE_DIR);
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}