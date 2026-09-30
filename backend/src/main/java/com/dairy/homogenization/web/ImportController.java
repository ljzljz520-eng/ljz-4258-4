package com.dairy.homogenization.web;

import com.dairy.homogenization.domain.ImportFile;
import com.dairy.homogenization.repository.ImportFileRepository;
import com.dairy.homogenization.repository.ImportJobRepository;
import com.dairy.homogenization.service.ImportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportService imports;
    private final ImportJobRepository jobs;
    private final ImportFileRepository files;
    private final ViewAssembler views;

    public ImportController(ImportService imports, ImportJobRepository jobs, ImportFileRepository files,
                            ViewAssembler views) {
        this.imports = imports; this.jobs = jobs; this.files = files; this.views = views;
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPERATOR','LAB_ANALYST')")
    public Object upload(@RequestParam("files") List<MultipartFile> uploads,
                         @RequestParam("type") String type, Principal principal) {
        return views.job(imports.createJob(uploads, type, principal.getName()));
    }

    @PostMapping("/process-next")
    @PreAuthorize("hasAnyRole('LAB_ANALYST')")
    public void process() { imports.processNextJob(); }

    @PostMapping("/files/{id}/retry")
    @PreAuthorize("hasAnyRole('OPERATOR','LAB_ANALYST')")
    public Object retry(@PathVariable Long id) {
        ImportFile f = imports.retryFile(id);
        return views.job(jobs.findById(f.getJobId()).orElseThrow());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Object get(@PathVariable Long id) { return views.job(jobs.findById(id).orElseThrow()); }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Object list() { return jobs.findAll().stream().map(views::job).toList(); }
}
