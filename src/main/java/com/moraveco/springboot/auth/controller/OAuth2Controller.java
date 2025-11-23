package com.moraveco.springboot.auth.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
@CrossOrigin(origins = "*")
public class OAuth2Controller {

    @GetMapping("/login/google")
    public Map<String, String> getGoogleLoginUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("url", "http://localhost:8080/oauth2/authorization/google");
        return response;
    }
}