package com.example.dairy.web;

import com.example.dairy.domain.ImportJob;
import com.example.dairy.dto.Dtos.FileSummary;
import com.example.dairy.dto.Dtos.ImportSummary;
import com.example.dairy.service.ImportService;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/import-jobs/{jobId}")
public class ImportJobController {
    private final ImportService imports;
    public ImportJobController(ImportService imports) { this.imports = imports; }

    @GetMapping
    public ImportSummary get(@PathVariable UUID jobId) {
        ImportJob job = imports.getJob(jobId);
        return new ImportSummary(job.getStatus().name(), job.getTotalFiles(), job.getSuccessfulFiles(),
                job.getFailedFiles(), job.getFiles().stream()
                .map(f -> new FileSummary(f.getId(), f.getFilename(), f.getStatus().name(), f.getAttempts(),
                        f.getErrorMessage(), f.getProcessedAt())).toList());
    }
}
