CREATE TABLE valve_groups (
    id uuid PRIMARY KEY,
    code text NOT NULL UNIQUE,
    name text NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sampling_lines (
    id uuid PRIMARY KEY,
    valve_group_id uuid NOT NULL REFERENCES valve_groups(id),
    code text NOT NULL UNIQUE,
    name text NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE product_batches (
    id uuid PRIMARY KEY,
    batch_no text NOT NULL UNIQUE,
    product_name text NOT NULL,
    produced_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE instrument_algorithm_versions (
    id uuid PRIMARY KEY,
    instrument_code text NOT NULL,
    algorithm_name text NOT NULL,
    version text NOT NULL,
    checksum text,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(instrument_code, algorithm_name, version)
);

CREATE TABLE measurement_points (
    id uuid PRIMARY KEY,
    code text NOT NULL UNIQUE,
    name text NOT NULL,
    unit text NOT NULL DEFAULT 'bar'
);

CREATE TABLE pressure_calibration_mappings (
    id uuid PRIMARY KEY,
    valve_group_id uuid NOT NULL REFERENCES valve_groups(id),
    measurement_point_id uuid NOT NULL REFERENCES measurement_points(id),
    instrument_label text NOT NULL,
    canonical_stage_label text NOT NULL,
    correction_level integer NOT NULL DEFAULT 0,
    version integer NOT NULL,
    slope numeric(12,6) NOT NULL,
    intercept numeric(12,6) NOT NULL DEFAULT 0,
    effective_from timestamptz NOT NULL,
    active boolean NOT NULL DEFAULT true,
    supersedes_id uuid REFERENCES pressure_calibration_mappings(id),
    created_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(valve_group_id, measurement_point_id, instrument_label, version)
);
CREATE INDEX idx_pressure_mapping_lookup
    ON pressure_calibration_mappings(valve_group_id, measurement_point_id, instrument_label, active);

CREATE TABLE batch_segments (
    id uuid PRIMARY KEY,
    valve_group_id uuid NOT NULL REFERENCES valve_groups(id),
    sampling_line_id uuid NOT NULL REFERENCES sampling_lines(id),
    product_batch_id uuid NOT NULL REFERENCES product_batches(id),
    code text NOT NULL UNIQUE,
    target_stage_label text NOT NULL,
    baseline_start timestamptz NOT NULL,
    baseline_end timestamptz NOT NULL,
    stable_start timestamptz NOT NULL,
    stable_end timestamptz NOT NULL,
    status text NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','LOCKED','ISSUED')),
    lock_version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (baseline_start < baseline_end AND stable_start < stable_end)
);
CREATE INDEX idx_segments_batch ON batch_segments(product_batch_id);
CREATE INDEX idx_segments_line ON batch_segments(sampling_line_id);

CREATE TABLE import_jobs (
    id uuid PRIMARY KEY,
    segment_id uuid NOT NULL REFERENCES batch_segments(id),
    status text NOT NULL CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','COMPLETED_WITH_FAILURES','FAILED','CANCELLED')),
    total_files integer NOT NULL,
    successful_files integer NOT NULL DEFAULT 0,
    failed_files integer NOT NULL DEFAULT 0,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    finished_at timestamptz,
    locked_at timestamptz,
    locked_by text
);
CREATE INDEX idx_import_jobs_segment_status ON import_jobs(segment_id, status);

CREATE TABLE import_files (
    id uuid PRIMARY KEY,
    job_id uuid NOT NULL REFERENCES import_jobs(id),
    filename text NOT NULL,
    checksum text NOT NULL,
    content bytea NOT NULL,
    status text NOT NULL CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
    attempts integer NOT NULL DEFAULT 0,
    error_message text,
    locked_at timestamptz,
    locked_by text,
    processed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_import_files_job_status ON import_files(job_id, status);
CREATE UNIQUE INDEX uq_import_checksum_success ON import_files(checksum) WHERE status = 'SUCCEEDED';

CREATE TABLE samples (
    id uuid PRIMARY KEY,
    segment_id uuid NOT NULL REFERENCES batch_segments(id),
    sampling_line_id uuid NOT NULL REFERENCES sampling_lines(id),
    product_batch_id uuid NOT NULL REFERENCES product_batches(id),
    measurement_point_id uuid NOT NULL REFERENCES measurement_points(id),
    algorithm_version_id uuid NOT NULL REFERENCES instrument_algorithm_versions(id),
    import_file_id uuid REFERENCES import_files(id),
    sample_code text NOT NULL UNIQUE,
    sample_type text NOT NULL CHECK (sample_type IN ('BEFORE','AFTER')),
    layer text NOT NULL CHECK (layer IN ('TOP','MIDDLE','BOTTOM')),
    sampled_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_samples_pairing ON samples(segment_id, sample_type, layer, sampled_at);
CREATE INDEX idx_samples_received ON samples(received_at);

CREATE TABLE pressure_readings (
    id uuid PRIMARY KEY,
    sample_id uuid NOT NULL REFERENCES samples(id) ON DELETE CASCADE,
    stage_order integer NOT NULL,
    raw_label text NOT NULL,
    raw_value_bar numeric(12,4) NOT NULL,
    mapping_id uuid REFERENCES pressure_calibration_mappings(id),
    corrected_value_bar numeric(12,4),
    measured_at timestamptz NOT NULL,
    UNIQUE(sample_id, stage_order)
);
CREATE INDEX idx_pressure_readings_mapping ON pressure_readings(mapping_id);

CREATE TABLE particle_distributions (
    id uuid PRIMARY KEY,
    sample_id uuid NOT NULL REFERENCES samples(id) ON DELETE CASCADE,
    bin_um numeric(12,4) NOT NULL,
    volume_pct numeric(9,6) NOT NULL CHECK (volume_pct >= 0 AND volume_pct <= 100),
    UNIQUE(sample_id, bin_um)
);

CREATE TABLE comparisons (
    id uuid PRIMARY KEY,
    segment_id uuid NOT NULL REFERENCES batch_segments(id),
    before_sample_id uuid NOT NULL REFERENCES samples(id),
    after_sample_id uuid NOT NULL REFERENCES samples(id),
    pressure_mapping_id uuid REFERENCES pressure_calibration_mappings(id),
    status text NOT NULL CHECK (status IN ('PROPOSED','BLOCKED','STALE','CONFIRMED','SUPERSEDED')),
    auto_generated boolean NOT NULL DEFAULT true,
    proposed_by text NOT NULL,
    proposed_at timestamptz NOT NULL DEFAULT now(),
    confirmed_by text,
    confirmed_at timestamptz,
    lock_version bigint NOT NULL DEFAULT 0,
    CHECK (before_sample_id <> after_sample_id)
);
CREATE UNIQUE INDEX uq_active_after_candidate
    ON comparisons(after_sample_id) WHERE status IN ('PROPOSED','BLOCKED','STALE','CONFIRMED');
CREATE INDEX idx_comparisons_segment_status ON comparisons(segment_id, status);

CREATE TABLE comparison_reasons (
    id uuid PRIMARY KEY,
    comparison_id uuid NOT NULL REFERENCES comparisons(id) ON DELETE CASCADE,
    code text NOT NULL,
    detail text NOT NULL,
    severity text NOT NULL DEFAULT 'BLOCKER' CHECK (severity IN ('BLOCKER','WARNING'))
);
CREATE INDEX idx_comparison_reasons ON comparison_reasons(comparison_id);

CREATE TABLE comparison_dependencies (
    comparison_id uuid PRIMARY KEY REFERENCES comparisons(id) ON DELETE CASCADE,
    pressure_mapping_id uuid REFERENCES pressure_calibration_mappings(id),
    particle_algorithm_before_id uuid NOT NULL REFERENCES instrument_algorithm_versions(id),
    particle_algorithm_after_id uuid NOT NULL REFERENCES instrument_algorithm_versions(id)
);
CREATE INDEX idx_comparison_dependency_mapping ON comparison_dependencies(pressure_mapping_id);

CREATE TABLE pressure_mapping_revisions (
    id uuid PRIMARY KEY,
    old_mapping_id uuid REFERENCES pressure_calibration_mappings(id),
    new_mapping_id uuid NOT NULL REFERENCES pressure_calibration_mappings(id),
    affected_segment_id uuid NOT NULL REFERENCES batch_segments(id),
    correction_level integer NOT NULL,
    reason text NOT NULL,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE evidence_snapshots (
    id uuid PRIMARY KEY,
    segment_id uuid NOT NULL REFERENCES batch_segments(id),
    payload jsonb NOT NULL,
    sha256 text NOT NULL UNIQUE,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE reviews (
    id uuid PRIMARY KEY,
    segment_id uuid NOT NULL UNIQUE REFERENCES batch_segments(id),
    status text NOT NULL CHECK (status IN ('LOCKED','ISSUED','RELEASED')),
    evidence_snapshot_id uuid REFERENCES evidence_snapshots(id),
    locked_by text NOT NULL,
    locked_at timestamptz NOT NULL DEFAULT now(),
    decision_by text,
    decision_at timestamptz,
    decision text CHECK (decision IS NULL OR decision IN ('APPROVED','REJECTED')),
    notes text,
    lock_version bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_reviews_status ON reviews(status);

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY,
    actor text NOT NULL,
    action text NOT NULL,
    segment_id uuid REFERENCES batch_segments(id),
    detail jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);
