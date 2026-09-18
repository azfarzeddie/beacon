package com.beacon.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkNotificationRequest {
    @Valid
    @NotEmpty
    private List<SendNotificationRequest> notifications;
}
