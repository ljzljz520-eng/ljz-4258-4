package com.example.dairy.web;

import com.example.dairy.domain.ImportFile;
import com.example.dairy.domain.ImportJob;
import com.example.dairy.dto.Dtos.FileSummary;
import com.example.dairy.dto.Dtos.IdResponse;
import com.example.dairy.dto.Dtos.ImportSummary;
import com.example.dairy.service.ImportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/segments/{segmentId}/imports")
public class ImportController {
    private final ImportService imports;
    public ImportController(ImportService imports) { this.imports = imports; }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public IdResponse upload(@PathVariable UUID segmentId, @RequestParam("files") List<MultipartFile> files, Authentication auth) {
        return new IdResponse(imports.createJob(segmentId, files, auth.getName()).getId());
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('OPERATOR')")
    public IdResponse processNow(@PathVariable UUID segmentId) {
        imports.processPendingJobs(10, "api-worker");
        return new IdResponse(segmentId);
    }
}
