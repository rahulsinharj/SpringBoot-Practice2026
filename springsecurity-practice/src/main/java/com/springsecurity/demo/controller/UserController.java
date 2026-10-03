package com.springsecurity.demo.controller;

import com.springsecurity.demo.dto.UserRegisterResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello";
    }

    @PostMapping
    public ResponseEntity<UserRegisterResponseDto> registerUser() {
        // Implement user registration logic here
        UserRegisterResponseDto responseDto = new UserRegisterResponseDto();
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // Implement user deletion logic here
        return ResponseEntity.noContent().build();
    }

}
