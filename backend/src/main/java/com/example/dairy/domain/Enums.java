package com.example.dairy.domain;

public final class Enums {
    private Enums() {}
    public enum SegmentStatus { OPEN, LOCKED, ISSUED }
    public enum ImportStatus { PENDING, RUNNING, SUCCEEDED, COMPLETED_WITH_FAILURES, FAILED, CANCELLED }
    public enum FileStatus { PENDING, RUNNING, SUCCEEDED, FAILED, CANCELLED }
    public enum SampleType { BEFORE, AFTER }
    public enum Layer { TOP, MIDDLE, BOTTOM }
    public enum ComparisonStatus { PROPOSED, BLOCKED, STALE, CONFIRMED, SUPERSEDED }
    public enum ReviewStatus { LOCKED, ISSUED, RELEASED }
    public enum ReviewDecision { APPROVED, REJECTED }
    public enum Severity { BLOCKER, WARNING }
}
