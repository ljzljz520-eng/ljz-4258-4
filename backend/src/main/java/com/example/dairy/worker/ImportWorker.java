package com.example.dairy.worker;

import com.example.dairy.service.ImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImportWorker {
    private final ImportService imports;
    @Value("${app.worker.enabled:true}")
    private boolean enabled;

    public ImportWorker(ImportService imports) { this.imports = imports; }

    @Scheduled(fixedDelayString = "${app.worker.poll-ms:2000}")
    public void poll() {
        if (enabled) imports.processPendingJobs(1, "worker-1");
    }
}
