package com.moraveco.springboot.account.service;

import com.moraveco.springboot.account.entity.BlockedUser;
import com.moraveco.springboot.auth.entity.User;
import com.moraveco.springboot.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return userRepository.findAllUsersCustom();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findUserById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // NEW: Get users that have conversations with the current user
    public List<User> getUsersWithConversations(String currentUserUid) {
        return userRepository.findUsersWithConversations(currentUserUid);
    }

    public List<BlockedUser> getBlockedUsers(String currentUserUid) {
        return userRepository.getBlockedUsers(currentUserUid);
    }

    public List<User> searchUsers(String query) {
        return userRepository.searchUsers(query);
    }

    // NEW: Update user profile (name and lastname)
    public User updateUserProfile(String id, String name, String lastname) {
        Optional<User> userOptional = userRepository.findUserById(id);
        if (userOptional.isPresent()) {
            userRepository.updateUserProfile(id, name, lastname);
            // Return updated user
            return userRepository.findUserById(id).orElseThrow(() ->
                    new RuntimeException("User not found after update"));
        } else {
            throw new RuntimeException("User not found with id: " + id);
        }
    }

    // NEW: Update user profile image
    public User updateUserProfileImage(String id, String imageUrl) {
        Optional<User> userOptional = userRepository.findUserById(id);
        if (userOptional.isPresent()) {
            userRepository.updateUserProfileImage(id, imageUrl);
            // Return updated user
            return userRepository.findUserById(id).orElseThrow(() ->
                    new RuntimeException("User not found after update"));
        } else {
            throw new RuntimeException("User not found with id: " + id);
        }
    }

    // NEW: Update complete user profile
    public User updateCompleteProfile(String userId, String name, String lastname, String imageUrl) {
        // Find user by ID
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Log current state
        System.out.println("=== BEFORE UPDATE ===");
        System.out.println("Current name: " + user.getName());
        System.out.println("Current lastname: " + user.getLastname());
        System.out.println("Current profileImage: " + user.getProfileImage());

        // Update fields
        user.setName(name);
        user.setLastname(lastname);

        if (imageUrl != null) {
            user.setProfileImage(imageUrl);
        }

        // Log what we're about to save
        System.out.println("=== ABOUT TO SAVE ===");
        System.out.println("New name: " + user.getName());
        System.out.println("New lastname: " + user.getLastname());
        System.out.println("New profileImage: " + user.getProfileImage());

        // Save
        return userRepository.save(user);
    }

    /**
     * Zablokuje uživatele targetUserId pro uživatele userId.
     * Idempotentní: pokud je již zablokován, neprovádí žádnou změnu.
     * Vyhazuje IllegalArgumentException, pokud některý z uživatelů neexistuje
     * nebo pokud userId == targetUserId.
     */
    @Transactional
    public void blockUser(String userId, String targetUserId) {
        if (userId == null || targetUserId == null || userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Neplatná vstupní data pro blokaci.");
        }

        // Ověření existence obou uživatelů
        userRepository.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen: " + userId));
        userRepository.findUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen: " + targetUserId));

        // Idempotence: je-li už blokován, nedělej nic
        boolean alreadyBlocked = userRepository.isBlocked(userId, targetUserId) > 0;
        if (alreadyBlocked) {
            return;
        }

        // Proveď blokaci
        userRepository.blockUser(userId, targetUserId);
    }

    /**
     * Odblokuje uživatele targetUserId pro uživatele userId.
     * Idempotentní: pokud blokace neexistuje, neprovádí žádnou změnu.
     * Vyhazuje IllegalArgumentException, pokud některý z uživatelů neexistuje
     * nebo pokud userId == targetUserId.
     */
    @Transactional
    public void unblockUser(String userId, String targetUserId) {
        if (userId == null || targetUserId == null || userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Neplatná vstupní data pro odblokování.");
        }

        // Ověření existence obou uživatelů
        userRepository.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen: " + userId));
        userRepository.findUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen: " + targetUserId));

        // Idempotence: pokud není blokován, nedělej nic
        boolean alreadyBlocked = userRepository.isBlocked(userId, targetUserId) > 0;
        if (!alreadyBlocked) {
            return;
        }

        // Proveď odblokování
        userRepository.unblockUser(userId, targetUserId);
    }

}