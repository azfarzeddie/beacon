package com.beacon.service.stub;

import com.beacon.model.Types.Channel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Inspection endpoints for end-to-end tests. Only registered when at least one channel
 * is using a stub provider, so it can never be exposed in a real-provider deployment.
 */
@RestController
@RequestMapping("/stub")
@ConditionalOnExpression("'${spring.sms.provider:}' == 'stub' or '${spring.push.provider:}' == 'stub' or '${spring.mail.provider:}' == 'stub'")
public class StubController {
    private final StubGateway gateway;

    public StubController(StubGateway gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/notifications")
    public List<StubMessage> notifications(@RequestParam(required = false) Channel channel,
                                           @RequestParam(required = false) String to,
                                           @RequestParam(defaultValue = "100") int limit) {
        return gateway.find(channel, to, limit);
    }

    @GetMapping("/stats")
    public Map<Channel, StubGateway.ChannelStats> stats() {
        return gateway.stats();
    }

    @DeleteMapping("/notifications")
    public ResponseEntity<Void> reset() {
        gateway.reset();
        return ResponseEntity.noContent().build();
    }
}
