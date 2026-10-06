package com.beacon.service;

import com.beacon.model.entity.DeviceToken;
import com.beacon.model.entity.User;
import com.beacon.model.request.CreateUserRequest;
import com.beacon.model.response.CreateUserResponse;
import com.beacon.model.response.GetUserResponse;
import com.beacon.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
        return CreateUserResponse.from(user);
    }

    @Transactional
    public GetUserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user with ID: " + id + " found."));

        return GetUserResponse.from(user);
    }

    @Transactional
    public CreateUserResponse updateUser(Long id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user with ID: " + id + " found."));

        Optional<User> otherExtId = userRepository.findByExternalId(request.getExternalId());
        if (otherExtId.isPresent() && !otherExtId.get().equals(user)) {
            throw new UserAlreadyExistsException("User with external ID: "
                    + request.getExternalId() + " already exists.");
        }
        Optional<User> otherEmail = userRepository.findByEmail(request.getEmail());
        if (otherEmail.isPresent() && !otherEmail.get().equals(user)) {
            throw new UserAlreadyExistsException("User with email: " + request.getEmail() + " already exists.");
        }

        user.setExternalId(request.getExternalId());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        userRepository.save(user);
        return CreateUserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user with ID: " + id + " found."));

        userRepository.deleteById(id);
    }
}
