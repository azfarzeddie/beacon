package com.beacon.service;

import com.beacon.model.Types.Channel;
import com.beacon.model.entity.User;
import com.beacon.model.entity.UserPreference;
import com.beacon.model.request.CreateUserPreferenceRequest;
import com.beacon.model.response.CreateUserPreferenceResponse;
import com.beacon.model.response.GetPreferenceResponse;
import com.beacon.model.response.GetPreferencesResponse;
import com.beacon.repository.PreferenceRepository;
import com.beacon.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.beacon.exception.PreferenceException.*;
import static com.beacon.exception.UserException.*;

@Slf4j
@Service
public class PreferencesService {
    private final UserRepository userRepository;
    private final PreferenceRepository preferenceRepository;

    @Autowired
    public PreferencesService(UserRepository userRepository, PreferenceRepository preferenceRepository) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
    }

    public CreateUserPreferenceResponse addUserPreference(CreateUserPreferenceRequest request) {
        // check if the userId is valid.
        Optional<User> userResponse = userRepository.findByExternalId(request.getUserExternalId());
        if (userResponse.isEmpty()) {
            throw new UserNotFoundException("No user with externalId " + request.getUserExternalId() + " exists.");
        }
        User user = userResponse.get();

        // check if a preference with this type and channel already exists for this user
        String notificationType = request.getNotificationType();
        Channel channel = request.getChannel();
        if (preferenceRepository.findByUserIdAndNotificationTypeAndChannel(user.getId(), notificationType, channel).isPresent()) {
            throw new PreferenceAlreadyExists("A notification preference for " +
                    notificationType + " and " + channel + " already exists. Please call PUT endpoint to update.");
        }

        // create preference
        UserPreference preference = new UserPreference();
        preference.setUserId(user.getId());
        preference.setNotificationType(notificationType);
        preference.setChannel(channel);
        preference.setPreference(request.getPreference());

        preferenceRepository.save(preference);
        return new CreateUserPreferenceResponse(preference.getId(), preference.getUserId(),
                preference.getNotificationType(), preference.getChannel(), preference.getCreatedAt(),
                preference.getUpdatedAt());
    }

    public GetPreferencesResponse getAllUserPreferences(String userId) {
        // check if the userId is valid.
        User user = checkUserExists(userId);

        Optional<List<UserPreference>> preferenceResponse = preferenceRepository.findByUserId(user.getId());
        List<GetPreferenceResponse> notificationPreferences = getNotificationPreferences(userId, preferenceResponse);
        return GetPreferencesResponse.builder()
                .userId(user.getId())
                .preferences(notificationPreferences)
                .build();
    }

    public GetPreferenceResponse getUserPreference(String userId, UUID id) {
        // check if the userId is valid.
        User user = checkUserExists(userId);

        Optional<UserPreference> preferenceResponse = preferenceRepository.findByIdAndUserId(id, user.getId());
        if (preferenceResponse.isEmpty()) {
            throw new PreferenceNotFound("No preference found for user with ID: " + userId);
        }
        UserPreference preference = preferenceResponse.get();
        return new GetPreferenceResponse(preference.getId(), preference.getNotificationType(), preference.getChannel(),
                preference.getPreference(), preference.isActive(), preference.getCreatedAt(), preference.getUpdatedAt());
    }

    private static @NonNull List<GetPreferenceResponse> getNotificationPreferences(String userId, Optional<List<UserPreference>> preferenceResponse) {
        if (preferenceResponse.isEmpty()) {
            throw new PreferenceNotFound("No preference found for user with ID: " + userId);
        }
        List<UserPreference> preferences = preferenceResponse.get();
        List<GetPreferenceResponse> notificationPreferences = new ArrayList<>();
        preferences.forEach(e -> {
            notificationPreferences.add(new GetPreferenceResponse(e.getId(), e.getNotificationType(), e.getChannel(),
                    e.getPreference(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt()));
        });
        return notificationPreferences;
    }

    private User checkUserExists(String userId) {
        Optional<User> userResponse = userRepository.findByExternalId(userId);
        if (userResponse.isEmpty()) {
            throw new UserNotFoundException("No user with externalId " + userId + " exists.");
        }
        return userResponse.get();
    }
}
