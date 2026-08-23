package com.beacon.controller;

import com.beacon.model.request.CreateTemplateRequest;
import com.beacon.model.response.CreateTemplateResponse;
import com.beacon.model.response.GetTemplateResponse;
import com.beacon.service.TemplateService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static com.beacon.model.Types.Channel;

@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {
    private final TemplateService templateService;

    @Autowired
    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    ResponseEntity<CreateTemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        CreateTemplateResponse response = templateService.createTemplate(request);
        URI location = URI.create("/api/v1/templates/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{notificationType}/{channel}")
    ResponseEntity<GetTemplateResponse> getTemplate(@PathVariable String notificationType, @PathVariable Channel channel) {
        GetTemplateResponse template = templateService.getTemplate(notificationType, channel);
        return ResponseEntity.ok(template);
    }
}
