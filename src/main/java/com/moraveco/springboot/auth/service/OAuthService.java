package com.moraveco.springboot.auth.service;

import com.moraveco.springboot.auth.entity.Login;
import com.moraveco.springboot.auth.entity.User;
import com.moraveco.springboot.auth.repository.LoginRepository;
import com.moraveco.springboot.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OAuthService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginRepository loginRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        // Check if user already exists
        Login existingLogin = loginRepository.findByEmail(email).orElse(null);

        if (existingLogin == null) {
            // Create new user
            String userId = UUID.randomUUID().toString();

            Login newLogin = new Login();
            newLogin.setId(userId);
            newLogin.setEmail(email);
            newLogin.setPassword(""); // OAuth users don't have a password
            newLogin.setEmailVerified(true); // Google already verified
            loginRepository.save(newLogin);

            User newUser = new User();
            newUser.setId(userId);
            newUser.setName(name != null ? name : "User");
            newUser.setLastname(""); // Google doesn't provide last name separately
            newUser.setProfileImage(picture);
            userRepository.save(newUser);
        } else {
            // Update profile image if changed
            User existingUser = userRepository.findUserById(existingLogin.getId()).orElse(null);
            if (existingUser != null && picture != null) {
                existingUser.setProfileImage(picture);
                userRepository.save(existingUser);
            }
        }

        return oauth2User;
    }
}