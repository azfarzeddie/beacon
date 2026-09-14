package com.beacon.controller;

import com.beacon.model.request.CreateUserPreferenceRequest;
import com.beacon.model.response.CreateUserPreferenceResponse;
import com.beacon.model.response.GetPreferenceResponse;
import com.beacon.model.response.GetPreferencesResponse;
import com.beacon.service.PreferencesService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @PostMapping
    ResponseEntity<CreateUserPreferenceResponse> createUserPreference(@Valid @RequestBody CreateUserPreferenceRequest request) {
        CreateUserPreferenceResponse response = preferencesService.addUserPreference(request);
        URI location = URI.create("/api/v1/preferences/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{userExternalId}")
    ResponseEntity<GetPreferencesResponse> getAllUserPreferences(@PathVariable String userExternalId) {
        GetPreferencesResponse response = preferencesService.getAllUserPreferences(userExternalId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userExternalId}/{preferenceId}")
    ResponseEntity<GetPreferenceResponse> getUserPreference(@PathVariable String userExternalId, @PathVariable UUID preferenceId) {
        GetPreferenceResponse response = preferencesService.getUserPreference(userExternalId, preferenceId);
        return ResponseEntity.ok(response);
    }

}
