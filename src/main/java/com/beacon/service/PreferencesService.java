package com.beacon.service;

import com.beacon.model.Types.Channel;
import com.beacon.model.entity.User;
import com.beacon.model.entity.UserPreference;
import com.beacon.model.request.CreateUserPreferenceRequest;
import com.beacon.model.request.UpdateUserPreferenceRequest;
import com.beacon.model.response.CreateUserPreferenceResponse;
import com.beacon.model.response.GetPreferenceResponse;
import com.beacon.model.response.GetPreferencesResponse;
import com.beacon.repository.PreferenceRepository;
import com.beacon.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.beacon.exception.PreferenceException.PreferenceAlreadyExists;
import static com.beacon.exception.PreferenceException.PreferenceNotFound;
import static com.beacon.exception.UserException.UserNotFoundException;

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

    @Transactional
    public CreateUserPreferenceResponse addUserPreference(CreateUserPreferenceRequest request) {
        // check if the userId is valid.
        User user = checkUserExists(request.getUserExternalId());

        // check if a preference with this type and channel already exists for this user
        String notificationType = request.getNotificationType();
        Channel channel = request.getChannel();
        Optional<UserPreference> existing = preferenceRepository.findByUserIdAndNotificationTypeAndChannel(
                user.getId(), notificationType, channel);
        if (existing.isPresent() && existing.get().isActive()) {
            throw new PreferenceAlreadyExists("A notification preference for " +
                    notificationType + " and " + channel + " already exists. Please call PUT endpoint to update.");
        }

        // a soft-deleted row keeps its place in the (userId, notificationType, channel) unique
        // constraint, so it is revived in place - inserting alongside it would be rejected
        UserPreference preference = existing.orElseGet(UserPreference::new);
        preference.setUserId(user.getId());
        preference.setNotificationType(notificationType);
        preference.setChannel(channel);
        preference.setPreference(request.getPreference());
        preference.setActive(true);

        preferenceRepository.save(preference);
        return new CreateUserPreferenceResponse(preference.getId(), preference.getUserId(),
                preference.getNotificationType(), preference.getChannel(), preference.getCreatedAt(),
                preference.getUpdatedAt());
    }

    @Transactional
    public GetPreferenceResponse updateUserPreference(String userId, UUID id, UpdateUserPreferenceRequest request) {
        // check if the userId is valid.
        User user = checkUserExists(userId);

        UserPreference preference = activePreference(user, userId, id);
        preference.setPreference(request.getPreference());
        preferenceRepository.save(preference);

        return toResponse(preference);
    }

    @Transactional
    public void deleteUserPreference(String userId, UUID id) {
        // check if the userId is valid.
        User user = checkUserExists(userId);

        // soft delete: the row is kept so the notification history that references this
        // (type, channel) pairing stays explainable, and so it can be revived by a later create
        UserPreference preference = activePreference(user, userId, id);
        preference.setActive(false);
        preferenceRepository.save(preference);
    }

    public GetPreferencesResponse getAllUserPreferences(String userId) {
        // check if the userId is valid.
        User user = checkUserExists(userId);

        List<GetPreferenceResponse> notificationPreferences =
                preferenceRepository.findByUserIdAndActiveTrue(user.getId()).stream()
                        .map(PreferencesService::toResponse)
                        .toList();
        return GetPreferencesResponse.builder()
                .userId(user.getId())
                .preferences(notificationPreferences)
                .build();
    }

    public GetPreferenceResponse getUserPreference(String userId, UUID id) {
        // check if the userId is valid.
        User user = checkUserExists(userId);

        return toResponse(activePreference(user, userId, id));
    }

    // a soft-deleted preference is treated as gone, so it reads as a 404 rather than a stale row
    private UserPreference activePreference(User user, String userExternalId, UUID id) {
        return preferenceRepository.findByIdAndUserIdAndActiveTrue(id, user.getId())
                .orElseThrow(() -> new PreferenceNotFound("No preference found for user with ID: " + userExternalId));
    }

    private static @NonNull GetPreferenceResponse toResponse(UserPreference preference) {
        return new GetPreferenceResponse(preference.getId(), preference.getNotificationType(), preference.getChannel(),
                preference.getPreference(), preference.isActive(), preference.getCreatedAt(), preference.getUpdatedAt());
    }

    private User checkUserExists(String userId) {
        Optional<User> userResponse = userRepository.findByExternalId(userId);
        if (userResponse.isEmpty()) {
            throw new UserNotFoundException("No user with externalId " + userId + " exists.");
        }
        return userResponse.get();
    }
}
