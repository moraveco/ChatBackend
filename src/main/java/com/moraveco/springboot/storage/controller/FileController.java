package com.moraveco.springboot.storage.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class FileController {

    // Shared folder between backend and fileserver
    private static final String SHARED_ROOT = "/fileserver";
    private static final String IMAGE_DIR = SHARED_ROOT + "/uploads/images/";
    private static final String PROFILE_DIR = SHARED_ROOT + "/uploads/profiles/";

    // ------------------------------
    // Upload Chat Image
    // ------------------------------
    @PostMapping("/images")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return saveFile(IMAGE_DIR, file);
    }

    // ------------------------------
    // Serve Chat Image
    // ------------------------------
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        return serveFile(IMAGE_DIR, filename);
    }

    // ------------------------------
    // Upload Profile Image
    // ------------------------------
    @PostMapping("/profiles")
    public ResponseEntity<String> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        return saveFile(PROFILE_DIR, file);
    }

    // ------------------------------
    // Serve Profile Image
    // ------------------------------
    @GetMapping("/profiles/{filename:.+}")
    public ResponseEntity<Resource> serveProfileImage(@PathVariable String filename) {
        return serveFile(PROFILE_DIR, filename);
    }

    // ------------------------------
    // Helper: Save File
    // ------------------------------
    private ResponseEntity<String> saveFile(String baseDir, MultipartFile file) {
        try {
            System.out.println("=== FILE UPLOAD DEBUG ===");
            System.out.println("Base directory: " + baseDir);
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("File content type: " + file.getContentType());

            Path uploadPath = Paths.get(baseDir);
            System.out.println("Upload path: " + uploadPath.toAbsolutePath());
            System.out.println("Upload path exists: " + Files.exists(uploadPath));

            // Ensure directory exists
            if (!Files.exists(uploadPath)) {
                System.out.println("Creating directories...");
                Files.createDirectories(uploadPath);
                System.out.println("Directories created successfully");
            }

            // Check if we can write to the directory
            System.out.println("Directory is writable: " + Files.isWritable(uploadPath));

            // Save the file
            Path targetPath = uploadPath.resolve(file.getOriginalFilename());
            System.out.println("Target path: " + targetPath.toAbsolutePath());

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved successfully");

            // Verify the file was created
            System.out.println("File exists after save: " + Files.exists(targetPath));
            System.out.println("File size after save: " + Files.size(targetPath));

            return ResponseEntity.ok("Uploaded successfully: " + file.getOriginalFilename());
        } catch (IOException e) {
            System.err.println("=== FILE UPLOAD ERROR ===");
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload: " + e.getMessage());
        }
    }

    // ------------------------------
    // Helper: Serve File
    // ------------------------------
    private ResponseEntity<Resource> serveFile(String baseDir, String filename) {
        try {
            Path filePath = Paths.get(baseDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
