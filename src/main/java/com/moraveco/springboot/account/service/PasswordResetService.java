package com.moraveco.springboot.account.service;

import com.moraveco.springboot.auth.entity.Login;
import com.moraveco.springboot.auth.entity.PasswordResetToken;
import com.moraveco.springboot.auth.repository.LoginRepository;
import com.moraveco.springboot.auth.repository.PasswordResetTokenRepository;
import com.moraveco.springboot.auth.service.EmailVerificationService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final LoginRepository loginRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailVerificationService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordResetService(LoginRepository loginRepository, PasswordResetTokenRepository tokenRepository, EmailVerificationService emailService) {
        this.loginRepository = loginRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public String requestPasswordReset(String email) {
        Login user = loginRepository.findByEmail(email).orElse(null);
        if (user == null) return "If the email exists, a reset link has been sent.";

        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user, token);
        return "If the email exists, a reset link has been sent.";
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);

        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        Login user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(user);
        tokenRepository.delete(resetToken);
        return true;
    }
}