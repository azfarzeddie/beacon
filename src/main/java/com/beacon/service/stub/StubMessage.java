package com.beacon.service.stub;

import com.beacon.model.Types.Channel;

import java.time.Instant;

public record StubMessage(Instant at, Channel channel, String to, String subject, String body,
                          long latencyMs, boolean accepted) {
}
