package com.moraveco.springboot.auth.controller;

import com.moraveco.springboot.auth.entity.*;
import com.moraveco.springboot.auth.repository.LoginRepository;
import com.moraveco.springboot.auth.repository.PasswordResetTokenRepository;
import com.moraveco.springboot.auth.repository.UserRepository;
import com.moraveco.springboot.auth.repository.VerificationTokenRepository;
import com.moraveco.springboot.auth.service.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository resetRepository;

    @Autowired
    private EmailVerificationService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Register registerRequest) {
        // Check if email already exists
        if (loginRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        // Create login record
        String userId = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());

        Login login = new Login();
        login.setId(userId);
        login.setEmail(registerRequest.getEmail());
        login.setPassword(hashedPassword);
        login.setEmailVerified(false);
        loginRepository.save(login);

        // Create user record
        User user = new User();
        user.setId(userId);
        user.setName(registerRequest.getName());
        user.setLastname(registerRequest.getLastname());
        userRepository.save(user);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(login);
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(registerRequest, token);

        return ResponseEntity.ok("Registration successful. Please check your email to verify your account.");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        VerificationToken verificationToken = verificationTokenRepository
                .findByToken(token)
                .orElse(null);

        if (verificationToken == null) {
            return ResponseEntity.badRequest().body("Invalid verification token");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Verification token has expired");
        }

        Login user = verificationToken.getUser();
        user.setEmailVerified(true);
        loginRepository.save(user);

        verificationTokenRepository.delete(verificationToken);

        return ResponseEntity.ok("Email verified successfully! You can now log in.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login loginRequest) {
        Login stored = loginRepository.findByEmail(loginRequest.getEmail()).orElse(null);

        if (stored == null) {
            return ResponseEntity.badRequest().body("Invalid email or password");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), stored.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid email or password");
        }

        if (!stored.getEmailVerified()) {
            return ResponseEntity.badRequest().body("Please verify your email before logging in");
        }

        // Get user details
        User user = userRepository.findUserById(stored.getId()).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("userId", stored.getId());
        response.put("email", stored.getEmail());
        if (user != null) {
            response.put("name", user.getName());
            response.put("lastname", user.getLastname());
            response.put("profileImage", user.getProfileImage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Login user = loginRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Don't reveal if email exists for security
            return ResponseEntity.ok("If the email exists, a password reset link has been sent");
        }

        // Delete any existing reset tokens for this user
        resetRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user, token);

        return ResponseEntity.ok("If the email exists, a password reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters long");
        }

        PasswordResetToken resetToken = resetRepository
                .findByToken(token)
                .orElse(null);

        if (resetToken == null) {
            return ResponseEntity.badRequest().body("Invalid password reset token");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Password reset token has expired");
        }

        Login user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(user);

        resetRepository.delete(resetToken);

        return ResponseEntity.ok("Password reset successful! You can now log in with your new password.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Login user = loginRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("Email not found");
        }

        if (user.getEmailVerified()) {
            return ResponseEntity.badRequest().body("Email is already verified");
        }

        // Delete old verification tokens
        verificationTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationTokenRepository.save(verificationToken);

        // Get user details for email
        User userDetails = userRepository.findUserById(user.getId()).orElse(null);
        Register registerRequest = new Register();
        registerRequest.setEmail(user.getEmail());
        if (userDetails != null) {
            registerRequest.setName(userDetails.getName());
        }

        emailService.sendVerificationEmail(registerRequest, token);

        return ResponseEntity.ok("Verification email sent");
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = loginRepository.findByEmail(email).isPresent();
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}