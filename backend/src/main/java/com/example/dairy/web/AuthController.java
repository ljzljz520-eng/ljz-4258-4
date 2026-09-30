package com.example.dairy.web;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController @RequestMapping("/api/me")
public class AuthController {
 @GetMapping public Map<String,Object> me(Authentication auth) {
  return Map.of("name", auth.getName(), "roles", auth.getAuthorities().stream().map(Object::toString).toList());
 }
}
