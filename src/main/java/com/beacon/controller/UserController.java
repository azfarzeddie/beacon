package com.beacon.controller;

import com.beacon.model.request.CreateUserRequest;
import com.beacon.model.response.CreateUserResponse;
import com.beacon.model.response.GetUserResponse;
import com.beacon.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse user = userService.createUser(request);
        URI location = URI.create("/api/v1/users/" + user.getId());
        return ResponseEntity.created(location).body(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetUserResponse> getUser(@PathVariable Long id) {
        GetUserResponse user = userService.getUser(id);
        return ResponseEntity.ok(user);
    }
}
