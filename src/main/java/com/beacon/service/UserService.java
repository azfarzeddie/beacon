package com.beacon.service;

import com.beacon.model.entity.DeviceToken;
import com.beacon.model.entity.User;
import com.beacon.model.request.CreateUserRequest;
import com.beacon.model.response.CreateUserResponse;
import com.beacon.model.response.DeviceTokenResponse;
import com.beacon.model.response.GetUserResponse;
import com.beacon.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.beacon.exception.UserException.UserAlreadyExistsException;
import static com.beacon.exception.UserException.UserNotFoundException;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;

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

        request.getDeviceTokens().forEach(e -> {
            DeviceToken token = new DeviceToken();
            token.setToken(e.getToken());
            token.setPlatform(e.getPlatform());
            user.addDeviceToken(token);
        });

        userRepository.save(user);
        return new CreateUserResponse(user.getId(), user.getExternalId(), user.getName(), user.getEmail(), user.getPhone());
    }

    @Transactional
    public GetUserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user with ID: " + id + " found."));

        return GetUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .externalId(user.getExternalId())
                .deviceTokens(user.getDeviceTokens().stream()
                        .map(t -> new DeviceTokenResponse(t.getToken(), t.getPlatform()))
                        .toList())
                .build();
    }
}
