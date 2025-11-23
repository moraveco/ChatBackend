package com.moraveco.springboot.account.controller;

import com.moraveco.springboot.account.entity.BlockedUser;
import com.moraveco.springboot.auth.entity.User;
import com.moraveco.springboot.account.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    // Match the FileController's directory structure
    private static final String SHARED_ROOT = "/fileserver";
    private static final String PROFILE_UPLOAD_DIR = SHARED_ROOT + "/uploads/profiles/";
    private static final Logger log = LoggerFactory.getLogger(UserController.class);


    public UserController(UserService userService) {
        this.userService = userService;

        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(PROFILE_UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create profile upload directory", e);
        }
    }

    @GetMapping("/getAllUsers")
    public List<User> getAllUsers() {
        return userService.getUsers();
    }

    @GetMapping("/searchUsers")
    public List<User> searchUsers(@RequestParam String query) {
        return userService.searchUsers(query);
    }

    @GetMapping("/getBlockedUsers")
    public List<BlockedUser> getBlockedUsers(@RequestParam String currentUserUid) {
        try {
            return userService.getBlockedUsers(currentUserUid);
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error blocking user", e); // 🔥 print the stack trace
            return Collections.emptyList();
        }
    }

    @GetMapping("/conversationUsers")
    public List<User> getUsersWithConversations(@RequestParam String currentUserUid) {
        return userService.getUsersWithConversations(currentUserUid);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Zablokuje uživatele targetId pro uživatele id.
     * 201 Created při úspěchu, 400 pokud id == targetId, 404 pokud některý z uživatelů neexistuje.
     */
    @PostMapping("/{id}/blocks/{targetId}")
    public ResponseEntity<Void> blockUser(
            @PathVariable String id,
            @PathVariable String targetId
    ) {
        log.info("Received block request: {} -> {}", id, targetId);

        if (id == null || targetId == null || id.equals(targetId)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            userService.blockUser(id, targetId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error blocking user", e); // 🔥 print the stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Odblokuje uživatele targetId pro uživatele id.
     * 204 No Content při úspěchu, 404 pokud záznam neexistuje nebo uživatelé nejsou nalezeni.
     */
    @DeleteMapping("/{id}/blocks/{targetId}")
    public ResponseEntity<Void> unblockUser(
            @PathVariable String id,
            @PathVariable String targetId
    ) {
        if (id == null || targetId == null || id.equals(targetId)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            userService.unblockUser(id, targetId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // např. pokud záznam blokace neexistuje
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // JSON endpoint for text-only profile updates
    @PutMapping(value = "/{id}/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> updateUserProfileJson(
            @PathVariable String id,
            @RequestBody UserProfileUpdateDto profileDto) {

        try {
            if (profileDto.getName() == null || profileDto.getLastname() == null) {
                return ResponseEntity.badRequest().build();
            }
            User updatedUser = userService.updateUserProfile(id, profileDto.getName(), profileDto.getLastname());
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }


    // Separate endpoint for profile image only
    @PostMapping("/{id}/profileImage")
    public ResponseEntity<User> uploadProfileImage(
            @PathVariable String id,
            @RequestParam("profileImage") MultipartFile imageFile) {

        try {
            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().build();
            }

            String filename = generateUniqueFilename(imageFile.getOriginalFilename());
            Path filePath = saveFile(imageFile, filename);
            String imageUrl = "/uploads/profiles/" + filename;

            User updatedUser = userService.updateUserProfileImage(id, imageUrl);
            return ResponseEntity.ok(updatedUser);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // Main endpoint for complete profile update with optional image
    @RequestMapping(
            value = "/{id}/completeProfile",
            method = RequestMethod.PUT,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<User> updateCompleteProfile(
            @PathVariable String id,
            @RequestParam("name") String name,
            @RequestParam("lastname") String lastname,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {

        try {
            String imageUrl = null;
            if (profileImage != null && !profileImage.isEmpty()) {
                String contentType = profileImage.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    System.err.println("Invalid image type: " + contentType);
                    return ResponseEntity.badRequest().build();
                }
                String filename = generateUniqueFilename(profileImage.getOriginalFilename());
                saveFile(profileImage, filename);
                imageUrl = "/uploads/profiles/" + filename;

            }

            User updatedUser = userService.updateCompleteProfile(id, name, lastname, imageUrl);

            if (updatedUser == null) {
                System.err.println("Service returned null user");
                return ResponseEntity.notFound().build();
            }

            System.out.println("User updated successfully");
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // Alternative: Handle both JSON and Multipart in one endpoint
    @PutMapping(value = "/{id}/updateProfile")
    public ResponseEntity<User> updateProfile(
            @PathVariable String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) MultipartFile profileImage,
            @RequestBody(required = false) UserProfileUpdateDto profileDto) {

        try {
            String finalName = name != null ? name : (profileDto != null ? profileDto.getName() : null);
            String finalLastname = lastname != null ? lastname : (profileDto != null ? profileDto.getLastname() : null);

            if (finalName == null || finalLastname == null) {
                return ResponseEntity.badRequest().build();
            }

            String imageUrl = null;
            if (profileImage != null && !profileImage.isEmpty()) {
                String filename = generateUniqueFilename(profileImage.getOriginalFilename());
                saveFile(profileImage, filename);
                imageUrl = "/uploads/profiles/" + filename;
            }

            User updatedUser = userService.updateCompleteProfile(id, finalName, finalLastname, imageUrl);
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(Long.parseLong(id));
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            // If ID is UUID string, try different approach
            return ResponseEntity.badRequest().build();
        }
    }

    // Helper method to generate unique filename
    private String generateUniqueFilename(String originalFilename) {
        String fileExtension = ".jpg"; // default
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + fileExtension;
    }

    // Helper method to save file
    private Path saveFile(MultipartFile file, String filename) throws IOException {
        Path uploadPath = Paths.get(PROFILE_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }

    // DTO class for JSON profile updates
    public static class UserProfileUpdateDto {
        private String name;
        private String lastname;

        public UserProfileUpdateDto() {}

        public UserProfileUpdateDto(String name, String lastname) {
            this.name = name;
            this.lastname = lastname;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }
    }
}