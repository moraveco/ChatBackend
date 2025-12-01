package com.moraveco.springboot.util;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public class ImageUtils {

    public static boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    public static String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Ensure we don't overwrite files by using UUID
        return UUID.randomUUID().toString() + extension;
    }
}