package com.beacon.service.stub;

import com.beacon.model.Types.Channel;
import com.beacon.service.push.PushProvider;
import org.springframework.stereotype.Component;

/** Select with spring.push.provider=stub. */
@Component
public class StubPushProvider implements PushProvider {
    private final StubGateway gateway;

    public StubPushProvider(StubGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getProviderName() {
        return "stub";
    }

    @Override
    public boolean send(String to, String message) {
        return gateway.accept(Channel.PUSH, to, null, message);
    }
}
