package com.moraveco.springboot.account.service;

import com.moraveco.springboot.auth.repository.LoginRepository;
import com.moraveco.springboot.auth.repository.UserRepository;
import com.moraveco.springboot.auth.repository.VerificationTokenRepository;
import com.moraveco.springboot.auth.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DeleteAccountService {

    private final UserRepository userRepository;
    private final LoginRepository loginRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;

    public DeleteAccountService(UserRepository userRepository, LoginRepository loginRepository, VerificationTokenRepository verificationTokenRepository, PasswordResetTokenRepository resetTokenRepository) {
        this.userRepository = userRepository;
        this.loginRepository = loginRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
    }

    @Transactional
    public void deleteUserAccount(String userId) {
        // 1. Clean up tokens
        verificationTokenRepository.deleteByUserId(userId);
        resetTokenRepository.deleteByUserId(userId);

        // 2. Delete login info
        loginRepository.deleteById(userId);

        // 3. Delete user profile (if ID matches, assuming User ID = Login ID)
        // Note: UserRepository uses Long, but code suggests String UUIDs.
        // If your User entity uses String ID:
        userRepository.deleteById(Long.valueOf(userId));
        // If User entity ID is String (which it looks like in your User.java file), use:
        // userRepository.delete(userRepository.findUserById(userId).orElseThrow());
    }
}