package com.dairy.homogenization.service;

import com.dairy.homogenization.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImportWorker {
    private static final Logger log = LoggerFactory.getLogger(ImportWorker.class);
    private final ImportService importService;
    private final AppProperties properties;

    public ImportWorker(ImportService importService, AppProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.worker.interval-ms:5000}")
    public void poll() {
        if (!properties.getWorker().isEnabled()) return;
        try {
            importService.processNextJob();
        } catch (Exception e) {
            log.warn("仪器导入轮询失败，任务/文件保持可重试状态", e);
        }
    }
}
