package com.beacon.service;

import com.beacon.model.entity.DeviceToken;
import com.beacon.model.entity.User;
import com.beacon.model.request.CreateUserRequest;
import com.beacon.model.response.CreateUserResponse;
import com.beacon.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.beacon.exception.UserException.UserAlreadyExistsException;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        // check if a user with the same externalId exists in the table or not
        if (userRepository.findByExternalId(request.getExternalId()).isPresent()) {
            throw new UserAlreadyExistsException("A user with ID: " + request.getExternalId() + " already exists.");
        }

        User user = new User();
        user.setExternalId(request.getExternalId());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        request.getDeviceTokens().forEach(e -> {
            DeviceToken token = new DeviceToken();
            token.setToken(e.getToken());
            token.setPlatform(e.getPlatform());
            user.addDeviceToken(token);
        });

        userRepository.save(user);
        return new CreateUserResponse(user.getId(), user.getExternalId(), user.getName(), user.getEmail(), user.getPhone());
    }
}
