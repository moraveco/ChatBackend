package com.moraveco.springboot.auth.repository;

import com.moraveco.springboot.account.entity.BlockedUser;
import com.moraveco.springboot.auth.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users", nativeQuery = true)
    List<User> findAllUsersCustom();

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findUserById(@Param("id") String id);

    // NEW: Find users that have conversations with the current user
    @Query(value = "SELECT DISTINCT u.* FROM users u " +
            "WHERE u.id IN (" +
            "   SELECT DISTINCT m.sender_uid FROM messages m WHERE m.receiver_uid = :currentUserUid " +
            "   UNION " +
            "   SELECT DISTINCT m.receiver_uid FROM messages m WHERE m.sender_uid = :currentUserUid" +
            ")", nativeQuery = true)
    List<User> findUsersWithConversations(@Param("currentUserUid") String currentUserUid);


    @Query(value = "SELECT * FROM users " +
            "WHERE name LIKE CONCAT('%', :query, '%') " +
            "OR lastname LIKE CONCAT('%', :query, '%')",
            nativeQuery = true)
    List<User> searchUsers(@Param("query") String query);

    @Query(value = "SELECT * FROM blocked_users WHERE blocker_id = :currentUserUid;", nativeQuery = true)
    List<BlockedUser> getBlockedUsers(@Param("currentUserUid") String uid);



    // NEW: Update user profile information
    @Transactional
    @Modifying
    @Query(value = "UPDATE users SET name = :firstName, lastname = :lastName WHERE id = :id", nativeQuery = true)
    void updateUserProfile(@Param("id") String id,
                           @Param("firstName") String firstName,
                           @Param("lastName") String lastName);

    // NEW: Update user profile image
    @Transactional
    @Modifying
    @Query(value = "UPDATE users SET profile_image = :imageUrl WHERE id = :id", nativeQuery = true)
    void updateUserProfileImage(@Param("id") String id, @Param("imageUrl") String imageUrl);

    // NEW: Update complete user profile
    @Transactional
    @Modifying
    @Query(value = "UPDATE users SET name = :firstName, lastname = :lastName, profile_image = :imageUrl WHERE id = :id", nativeQuery = true)
    void updateCompleteProfile(@Param("id") String id,
                               @Param("firstName") String firstName,
                               @Param("lastName") String lastName,
                               @Param("imageUrl") String imageUrl);

    // NEW: Update profile without image change
    @Transactional
    @Modifying
    @Query(value = "UPDATE users SET name = :firstName, lastname = :lastName WHERE id = :id", nativeQuery = true)
    void updateProfileWithoutImage(@Param("id") String id,
                                   @Param("firstName") String firstName,
                                   @Param("lastName") String lastName);

    /**
     * Vrátí true, pokud uživatel blockerId blokuje uživatele blockedId.
     */
    @Query(value = "SELECT COUNT(*) FROM blocked_users WHERE blocker_id = :blockerId AND blocked_id = :blockedId",
            nativeQuery = true)
    long isBlocked(@Param("blockerId") String blockerId, @Param("blockedId") String blockedId);


    /**
     * Provede blokaci uživatele blockedId uživatelem blockerId.
     * Idempotentní díky INSERT IGNORE (MySQL).
     */
    @jakarta.transaction.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "INSERT IGNORE INTO blocked_users (id, blocker_id, blocked_id, blocked_at) VALUES (UUID(),:blockerId, :blockedId, NOW())", nativeQuery = true)
    void blockUser(@Param("blockerId") String blockerId, @Param("blockedId") String blockedId);

    /**
     * Odblokuje uživatele blockedId pro uživatele blockerId.
     */
    @jakarta.transaction.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM blocked_users WHERE blocker_id = :blockerId AND blocked_id = :blockedId", nativeQuery = true)
    void unblockUser(@Param("blockerId") String blockerId, @Param("blockedId") String blockedId);

}
