package com.example.dairy.service;

import com.example.dairy.domain.*;
import com.example.dairy.dto.Dtos.*;
import com.example.dairy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SegmentService {
    private final BatchSegmentRepository segments;
    private final SampleRepository samples;
    private final ComparisonRepository comparisons;
    private final ImportJobRepository jobs;
    private final ReviewRepository reviews;
    private final DtoAssembler assembler;

    public SegmentService(BatchSegmentRepository segments, SampleRepository samples, ComparisonRepository comparisons,
                          ImportJobRepository jobs, ReviewRepository reviews, DtoAssembler assembler) {
        this.segments = segments; this.samples = samples; this.comparisons = comparisons;
        this.jobs = jobs; this.reviews = reviews; this.assembler = assembler;
    }

    @Transactional(readOnly=true)
    public List<SegmentResponse> list() {
        return segments.findAllByOrderByCode().stream().map(assembler::segment).toList();
    }

    @Transactional(readOnly=true)
    public SegmentDetailResponse detail(UUID id) {
        BatchSegment segment = segments.findById(id).orElseThrow(() -> new NotFoundException("批段不存在"));
        var sampleDtos = samples.findBySegmentIdOrderBySampledAtAsc(id).stream().map(assembler::sample).toList();
        var comparisonDtos = comparisons.findBySegmentIdOrderByProposedAtDesc(id).stream().map(assembler::comparison).toList();
        List<ImportJob> importJobs = jobs.findBySegmentIdOrderByCreatedAtAsc(id);
        ImportSummary importSummary;
        if (importJobs.isEmpty()) importSummary = new ImportSummary("NONE",0,0,0,List.of());
        else {
            ImportJob newest = importJobs.get(importJobs.size()-1);
            importSummary = new ImportSummary(newest.getStatus().name(), newest.getTotalFiles(), newest.getSuccessfulFiles(),
                    newest.getFailedFiles(), newest.getFiles().stream()
                    .map(f -> new FileSummary(f.getId(), f.getFilename(), f.getStatus().name(), f.getAttempts(), f.getErrorMessage(), f.getProcessedAt()))
                    .toList());
        }
        ReviewSummary reviewSummary = reviews.findBySegmentId(id).map(r -> new ReviewSummary(r.getStatus().name(),
                r.getDecision()==null?null:r.getDecision().name(), r.getLockedBy(), r.getLockedAt(),
                r.getDecisionBy(), r.getDecisionAt(),
                r.getEvidenceSnapshot()==null?null:r.getEvidenceSnapshot().getSha256())).orElse(null);
        return new SegmentDetailResponse(assembler.segment(segment), sampleDtos, comparisonDtos, importSummary, reviewSummary);
    }
}
