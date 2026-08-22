package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateUserResponse {
    Long id;
    String externalId;
    String name;
    String email;
    String phone;
}
