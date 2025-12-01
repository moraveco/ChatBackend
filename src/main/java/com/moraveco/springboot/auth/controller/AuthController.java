package com.moraveco.springboot.auth.controller;

import com.moraveco.springboot.account.service.PasswordResetService;
import com.moraveco.springboot.auth.entity.*;
import com.moraveco.springboot.auth.repository.LoginRepository;
import com.moraveco.springboot.auth.repository.PasswordResetTokenRepository;
import com.moraveco.springboot.auth.repository.UserRepository;
import com.moraveco.springboot.auth.repository.VerificationTokenRepository;
import com.moraveco.springboot.auth.service.EmailVerificationService;
import com.moraveco.springboot.util.JwtUtils;
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

    @Autowired
    private JwtUtils jwtUtils;


    @Autowired
    private PasswordResetService passwordResetService; // NEW

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

        if (stored == null || !passwordEncoder.matches(loginRequest.getPassword(), stored.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid email or password");
        }

        if (!stored.getEmailVerified()) {
            return ResponseEntity.badRequest().body("Please verify your email before logging in");
        }

        User user = userRepository.findUserById(stored.getId()).orElse(null);

        // GENERATE TOKEN
        String token = jwtUtils.generateToken(stored.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("token", token); // <--- Send the token!
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
        String result = passwordResetService.requestPasswordReset(request.get("email"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Password too short");
        }

        boolean success = passwordResetService.resetPassword(token, newPassword);
        if (success) {
            return ResponseEntity.ok("Password reset successful.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }
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