package com.moraveco.springboot.auth.service;


import com.moraveco.springboot.auth.entity.Login;
import com.moraveco.springboot.auth.entity.Register;
import com.moraveco.springboot.auth.entity.User;
import com.moraveco.springboot.auth.entity.VerificationToken;
import com.moraveco.springboot.auth.repository.LoginRepository;
import com.moraveco.springboot.auth.repository.UserRepository;
import com.moraveco.springboot.auth.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final LoginRepository loginRepository;
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailVerificationService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(LoginRepository loginRepository, UserRepository userRepository,
                       VerificationTokenRepository tokenRepository, EmailVerificationService emailService) {
        this.loginRepository = loginRepository;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void registerUser(Register request) {
        if (loginRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        String userId = UUID.randomUUID().toString();

        // Save Login
        Login login = new Login();
        login.setId(userId);
        login.setEmail(request.getEmail());
        login.setPassword(passwordEncoder.encode(request.getPassword()));
        login.setEmailVerified(false);
        loginRepository.save(login);

        // Save User Profile
        User user = new User();
        user.setId(userId);
        user.setName(request.getName());
        user.setLastname(request.getLastname());
        userRepository.save(user);

        // Generate Token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(login);
        tokenRepository.save(verificationToken);

        // Send Email
        emailService.sendVerificationEmail(request, token);
    }
}
