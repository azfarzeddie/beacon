package com.beacon.service.stub;

import com.beacon.model.Types.Channel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * The fake "provider backend" shared by the stub email/SMS/push providers: it blocks the calling
 * thread for a realistic accept latency, randomly rejects a configurable fraction of messages,
 * and keeps a bounded in-memory record of what was "delivered" so end-to-end tests can assert on it.
 */
@Component
public class StubGateway {

    public record ChannelStats(long accepted, long failed, double meanLatencyMs) {
    }

    private final StubProperties properties;
    private final Deque<StubMessage> messages = new ArrayDeque<>();
    private final Map<Channel, LongAdder> accepted = new EnumMap<>(Channel.class);
    private final Map<Channel, LongAdder> failed = new EnumMap<>(Channel.class);
    private final Map<Channel, LongAdder> latencyTotalMs = new EnumMap<>(Channel.class);

    public StubGateway(StubProperties properties) {
        this.properties = properties;
        for (Channel channel : Channel.values()) {
            accepted.put(channel, new LongAdder());
            failed.put(channel, new LongAdder());
            latencyTotalMs.put(channel, new LongAdder());
        }
    }

    /** Simulates one provider API call. Returns true if the provider "accepted" the message. */
    public boolean accept(Channel channel, String to, String subject, String body) {
        StubProperties.Profile profile = properties.forChannel(channel);
        long latencyMs = sampleLatencyMs(profile);
        sleep(latencyMs);
        boolean ok = ThreadLocalRandom.current().nextDouble() >= profile.getFailureRate();

        (ok ? accepted : failed).get(channel).increment();
        latencyTotalMs.get(channel).add(latencyMs);
        record(new StubMessage(Instant.now(), channel, to, subject, body, latencyMs, ok));
        return ok;
    }

    static long sampleLatencyMs(StubProperties.Profile profile) {
        if (profile.getMedianMs() <= 0) {
            return 0;
        }
        double sample = profile.getMedianMs() * Math.exp(profile.getSigma() * ThreadLocalRandom.current().nextGaussian());
        long capped = Math.min((long) sample, profile.getMaxMs() > 0 ? profile.getMaxMs() : Long.MAX_VALUE);
        return Math.max(capped, 0);
    }

    private static void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void record(StubMessage message) {
        int retain = properties.getRetain();
        if (retain <= 0) {
            return;
        }
        synchronized (messages) {
            messages.addLast(message);
            while (messages.size() > retain) {
                messages.removeFirst();
            }
        }
    }

    /** Most recent messages first, optionally filtered by channel and/or recipient. */
    public List<StubMessage> find(Channel channel, String to, int limit) {
        List<StubMessage> result = new ArrayList<>();
        synchronized (messages) {
            var it = messages.descendingIterator();
            while (it.hasNext() && result.size() < limit) {
                StubMessage m = it.next();
                if ((channel == null || m.channel() == channel) && (to == null || to.equals(m.to()))) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    public Map<Channel, ChannelStats> stats() {
        Map<Channel, ChannelStats> result = new EnumMap<>(Channel.class);
        for (Channel channel : Channel.values()) {
            long ok = accepted.get(channel).sum();
            long bad = failed.get(channel).sum();
            long total = ok + bad;
            result.put(channel, new ChannelStats(ok, bad, total == 0 ? 0 : (double) latencyTotalMs.get(channel).sum() / total));
        }
        return result;
    }

    public void reset() {
        synchronized (messages) {
            messages.clear();
        }
        for (Channel channel : Channel.values()) {
            accepted.get(channel).reset();
            failed.get(channel).reset();
            latencyTotalMs.get(channel).reset();
        }
    }
}
