package com.beacon.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkNotificationRequest(@Valid @NotEmpty List<SendNotificationRequest> notifications) {
}
