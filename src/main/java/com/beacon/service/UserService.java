package com.beacon.service;

import com.beacon.exception.UserException;
import com.beacon.model.entity.DeviceToken;
import com.beacon.model.entity.User;
import com.beacon.model.request.CreateUserRequest;
import com.beacon.model.response.CreateUserResponse;
import com.beacon.model.response.GetUserResponse;
import com.beacon.repository.DeviceTokenRepository;
import com.beacon.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.beacon.exception.UserException.UserAlreadyExistsException;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Autowired
    public UserService(UserRepository userRepository, DeviceTokenRepository deviceTokenRepository) {
        this.userRepository = userRepository;
        this.deviceTokenRepository = deviceTokenRepository;
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

    public GetUserResponse getUser(Long id) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            throw new UserException.UserNotFoundException("No user with ID: " + id + " found.");
        }

        User user = found.get();
        GetUserResponse response = GetUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .externalId(user.getExternalId())
                .build();

        List<DeviceToken> tokensFound = deviceTokenRepository.findAllByUserId(response.getId());

        if (!tokensFound.isEmpty()) {
            response.setDeviceTokens(new ArrayList<>());
            tokensFound.forEach(t -> {
                CreateUserRequest.DeviceToken token = new CreateUserRequest.DeviceToken();
                token.setToken(t.getToken());
                token.setPlatform(t.getPlatform());
                response.getDeviceTokens().add(token);
            });
        }

        return response;
    }
}
