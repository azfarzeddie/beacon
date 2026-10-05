package com.beacon.service.stub;

import com.beacon.model.Types.Channel;
import com.beacon.service.sms.SmsProvider;
import org.springframework.stereotype.Component;

/** Select with spring.sms.provider=stub. */
@Component
public class StubSmsProvider implements SmsProvider {
    private final StubGateway gateway;

    public StubSmsProvider(StubGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getProviderName() {
        return "stub";
    }

    @Override
    public boolean send(String to, String message) {
        return gateway.accept(Channel.SMS, to, null, message);
    }
}
