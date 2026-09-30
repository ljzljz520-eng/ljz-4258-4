package com.example.dairy.worker;

import com.example.dairy.repository.BatchSegmentRepository;
import com.example.dairy.service.PairingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartupPairing implements ApplicationRunner {
    private final BatchSegmentRepository segments;
    private final PairingService pairing;
    @Value("${app.demo.auto-pair-on-startup:true}")
    private boolean enabled;

    public StartupPairing(BatchSegmentRepository segments, PairingService pairing) {
        this.segments = segments; this.pairing = pairing;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (enabled) segments.findAllByOrderByCode().forEach(s -> pairing.autoPair(s.getId(), "startup-scan"));
    }
}
