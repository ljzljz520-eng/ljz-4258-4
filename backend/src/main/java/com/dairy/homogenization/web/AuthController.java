package com.dairy.homogenization.web;

import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    @GetMapping("/me")
    public Map<String,Object> me(Principal principal) {
        if (principal == null) return Map.of("authenticated", false);
        return Map.of("authenticated", true, "name", principal.getName());
    }
}
