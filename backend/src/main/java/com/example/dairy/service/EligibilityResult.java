package com.example.dairy.service;

import com.example.dairy.domain.PressureCalibrationMapping;
import com.example.dairy.dto.Dtos.ReasonResponse;
import java.util.ArrayList;
import java.util.List;

public record EligibilityResult(List<ReasonResponse> reasons, PressureCalibrationMapping pressureMapping) {
    public boolean comparable() {
        return reasons.stream().noneMatch(r -> "BLOCKER".equals(r.severity()));
    }
    public List<ReasonResponse> blockerReasons() {
        return new ArrayList<>(reasons);
    }
}
