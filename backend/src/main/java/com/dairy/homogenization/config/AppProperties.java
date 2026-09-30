package com.dairy.homogenization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Worker worker = new Worker();
    private Pairing pairing = new Pairing();

    public Worker getWorker() { return worker; }
    public Pairing getPairing() { return pairing; }

    public static class Worker {
        private boolean enabled;
        private long intervalMs = 5000;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    }

    public static class Pairing {
        private int maxTransportDelayMinutes = 1440;
        private int stableWindowToleranceSeconds = 30;
        public int getMaxTransportDelayMinutes() { return maxTransportDelayMinutes; }
        public void setMaxTransportDelayMinutes(int value) { this.maxTransportDelayMinutes = value; }
        public int getStableWindowToleranceSeconds() { return stableWindowToleranceSeconds; }
        public void setStableWindowToleranceSeconds(int value) { this.stableWindowToleranceSeconds = value; }
    }
}
