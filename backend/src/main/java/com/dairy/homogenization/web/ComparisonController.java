package com.dairy.homogenization.web;

import com.dairy.homogenization.dto.Requests.ManualCandidateRequest;
import com.dairy.homogenization.repository.ComparisonCandidateRepository;
import com.dairy.homogenization.service.ComparisonRuleApplicationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {
    private final ComparisonRuleApplicationService service;
    private final ComparisonCandidateRepository repository;
    private final ViewAssembler views;

    public ComparisonController(ComparisonRuleApplicationService service,
                                ComparisonCandidateRepository repository, ViewAssembler views) {
        this.service = service; this.repository = repository; this.views = views;
    }

    @PostMapping("/auto-pair/batches/{batchId}")
    @PreAuthorize("hasAnyRole('LAB_ANALYST','REVIEWER')")
    public Object autoPair(@PathVariable Long batchId) {
        service.evaluateBatch(batchId, true);
        return service.listBatch(batchId).stream().map(views::comparison).toList();
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('LAB_ANALYST','REVIEWER')")
    public Object manual(@Valid @RequestBody ManualCandidateRequest request, Principal principal) {
        return views.comparison(service.createManualPair(request.beforeSampleId(), request.afterSampleId(),
                request.stageCode(), principal.getName()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('LAB_ANALYST','REVIEWER')")
    public Object confirm(@PathVariable Long id) {
        return views.comparison(service.confirm(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('LAB_ANALYST','REVIEWER')")
    public Object reject(@PathVariable Long id) {
        return views.comparison(service.reject(id));
    }

    @PostMapping("/{id}/recheck")
    @PreAuthorize("isAuthenticated()")
    public Object recheck(@PathVariable Long id) {
        return views.comparison(service.evaluateCandidate(repository.findById(id).orElseThrow()));
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("isAuthenticated()")
    public Object byBatch(@PathVariable Long batchId) {
        return service.listBatch(batchId).stream().map(views::comparison).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Object one(@PathVariable Long id) { return views.comparison(service.get(id)); }

    @GetMapping("/boundary")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> boundary() {
        return Map.of(
            "automatic", "系统按同批、同阀组、同取样管线、同分层和运输时延自动产生全部候选，并显示阻断原因；不自动签发。",
            "manual", "实验员/审核员可选择具体前样-后样、确认或退回候选；但不能覆盖任何强制不可比检查。",
            "safety", "平台只做证据复核与可视化，不推荐压力，也不向均质机发送控制指令。"
        );
    }
}
