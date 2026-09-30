CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  full_name VARCHAR(128) NOT NULL,
  role VARCHAR(32) NOT NULL CHECK (role IN ('OPERATOR','LAB_ANALYST','REVIEWER')),
  password_hash VARCHAR(255) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE valve_groups (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sample_points (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  valve_group_id BIGINT NOT NULL REFERENCES valve_groups(id),
  sampling_line VARCHAR(64) NOT NULL,
  product_flow_order INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE product_batches (
  id BIGSERIAL PRIMARY KEY,
  batch_number VARCHAR(96) NOT NULL UNIQUE,
  product_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(160) NOT NULL,
  layer_name VARCHAR(64) NOT NULL DEFAULT 'BULK',
  started_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ,
  status VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pressure_mapping_versions (
  id BIGSERIAL PRIMARY KEY,
  valve_group_id BIGINT NOT NULL REFERENCES valve_groups(id),
  version INTEGER NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED')),
  effective_from TIMESTAMPTZ NOT NULL,
  effective_to TIMESTAMPTZ,
  corrected_by VARCHAR(64),
  corrected_at TIMESTAMPTZ,
  change_summary VARCHAR(1024) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (valve_group_id, version)
);
CREATE UNIQUE INDEX ux_pressure_mapping_active ON pressure_mapping_versions(valve_group_id) WHERE status = 'ACTIVE';

CREATE TABLE pressure_mapping_items (
  id BIGSERIAL PRIMARY KEY,
  version_id BIGINT NOT NULL REFERENCES pressure_mapping_versions(id) ON DELETE CASCADE,
  raw_label VARCHAR(128) NOT NULL,
  calibrated_stage_code VARCHAR(96) NOT NULL,
  nominal_pressure_bar NUMERIC(10,3),
  display_order INTEGER NOT NULL DEFAULT 0,
  UNIQUE (version_id, raw_label)
);

CREATE TABLE stability_windows (
  id BIGSERIAL PRIMARY KEY,
  batch_id BIGINT NOT NULL REFERENCES product_batches(id) ON DELETE CASCADE,
  valve_group_id BIGINT NOT NULL REFERENCES valve_groups(id),
  stage_code VARCHAR(96) NOT NULL,
  started_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ NOT NULL,
  nominal_pressure_bar NUMERIC(10,3),
  notes VARCHAR(512),
  CHECK (ended_at > started_at)
);
CREATE INDEX ix_stability_batch ON stability_windows(batch_id);
CREATE INDEX ix_stability_time ON stability_windows(started_at, ended_at);

CREATE TABLE instrument_algorithm_versions (
  id BIGSERIAL PRIMARY KEY,
  instrument_code VARCHAR(64) NOT NULL,
  measurement_kind VARCHAR(32) NOT NULL DEFAULT 'PARTICLE_SIZE',
  algorithm_version VARCHAR(64) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
  effective_from TIMESTAMPTZ NOT NULL,
  effective_to TIMESTAMPTZ,
  description VARCHAR(512),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (instrument_code, measurement_kind, algorithm_version)
);
CREATE UNIQUE INDEX ux_algorithm_active
  ON instrument_algorithm_versions(instrument_code, measurement_kind)
  WHERE status = 'ACTIVE';

CREATE TABLE samples (
  id BIGSERIAL PRIMARY KEY,
  sample_code VARCHAR(96) NOT NULL UNIQUE,
  batch_id BIGINT NOT NULL REFERENCES product_batches(id),
  sample_point_id BIGINT NOT NULL REFERENCES sample_points(id),
  valve_group_id BIGINT NOT NULL REFERENCES valve_groups(id),
  position VARCHAR(16) NOT NULL CHECK (position IN ('BEFORE','AFTER')),
  layer_name VARCHAR(64) NOT NULL,
  sampled_at TIMESTAMPTZ NOT NULL,
  received_at TIMESTAMPTZ,
  transport_delay_seconds INTEGER,
  container_code VARCHAR(96),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_samples_batch_position_time ON samples(batch_id, position, sampled_at);
CREATE INDEX ix_samples_point_time ON samples(sample_point_id, sampled_at);

CREATE TABLE particle_measurements (
  id BIGSERIAL PRIMARY KEY,
  sample_id BIGINT NOT NULL REFERENCES samples(id) ON DELETE CASCADE,
  algorithm_version_id BIGINT NOT NULL REFERENCES instrument_algorithm_versions(id),
  import_file_id BIGINT,
  d10_um NUMERIC(12,4),
  d50_um NUMERIC(12,4),
  d90_um NUMERIC(12,4),
  mean_um NUMERIC(12,4),
  measured_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUPERSEDED','REJECTED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_active_particle_measurement ON particle_measurements(sample_id) WHERE status = 'ACTIVE';
CREATE INDEX ix_particle_algorithm ON particle_measurements(algorithm_version_id);

CREATE TABLE size_distribution_points (
  id BIGSERIAL PRIMARY KEY,
  measurement_id BIGINT NOT NULL REFERENCES particle_measurements(id) ON DELETE CASCADE,
  bin_size_um NUMERIC(12,4) NOT NULL,
  volume_fraction NUMERIC(12,8) NOT NULL,
  cumulative_fraction NUMERIC(12,8),
  display_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX ix_distribution_measurement ON size_distribution_points(measurement_id, display_order);

CREATE TABLE pressure_observations (
  id BIGSERIAL PRIMARY KEY,
  batch_id BIGINT NOT NULL REFERENCES product_batches(id) ON DELETE CASCADE,
  valve_group_id BIGINT NOT NULL REFERENCES valve_groups(id),
  raw_label VARCHAR(128) NOT NULL,
  calibrated_stage_code VARCHAR(96),
  pressure_mapping_version_id BIGINT REFERENCES pressure_mapping_versions(id),
  observed_pressure_bar NUMERIC(12,4),
  observed_at TIMESTAMPTZ NOT NULL,
  import_file_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_pressure_batch_time ON pressure_observations(batch_id, observed_at);
CREATE INDEX ix_pressure_stage ON pressure_observations(calibrated_stage_code, observed_at);

CREATE TABLE comparison_candidates (
  id BIGSERIAL PRIMARY KEY,
  before_sample_id BIGINT NOT NULL REFERENCES samples(id),
  after_sample_id BIGINT NOT NULL REFERENCES samples(id),
  batch_id BIGINT NOT NULL REFERENCES product_batches(id),
  valve_group_id BIGINT NOT NULL REFERENCES valve_groups(id),
  stage_code VARCHAR(96),
  before_pressure_mapping_version_id BIGINT REFERENCES pressure_mapping_versions(id),
  after_pressure_mapping_version_id BIGINT REFERENCES pressure_mapping_versions(id),
  before_algorithm_version_id BIGINT REFERENCES instrument_algorithm_versions(id),
  after_algorithm_version_id BIGINT REFERENCES instrument_algorithm_versions(id),
  before_measurement_id BIGINT REFERENCES particle_measurements(id),
  after_measurement_id BIGINT REFERENCES particle_measurements(id),
  status VARCHAR(24) NOT NULL DEFAULT 'CANDIDATE'
    CHECK (status IN ('CANDIDATE','CONFIRMED','REJECTED','EXPIRED','APPROVED')),
  origin VARCHAR(24) NOT NULL DEFAULT 'AUTO' CHECK (origin IN ('AUTO','MANUAL')),
  comparable BOOLEAN NOT NULL DEFAULT FALSE,
  reason_summary VARCHAR(2048),
  fingerprint VARCHAR(128),
  version INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (before_sample_id <> after_sample_id)
);
CREATE UNIQUE INDEX ux_comparison_pair ON comparison_candidates(before_sample_id, after_sample_id);
CREATE INDEX ix_comparison_batch_stage ON comparison_candidates(batch_id, stage_code, status);
CREATE INDEX ix_comparison_status ON comparison_candidates(status);

CREATE TABLE comparability_findings (
  id BIGSERIAL PRIMARY KEY,
  comparison_id BIGINT NOT NULL REFERENCES comparison_candidates(id) ON DELETE CASCADE,
  code VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL CHECK (severity IN ('BLOCKER','WARNING','PASS')),
  message VARCHAR(1024) NOT NULL,
  detail TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_findings_comparison ON comparability_findings(comparison_id, severity);

CREATE TABLE reviews (
  id BIGSERIAL PRIMARY KEY,
  comparison_id BIGINT NOT NULL UNIQUE REFERENCES comparison_candidates(id),
  status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING','APPROVED','RETURNED')),
  requested_by VARCHAR(64) NOT NULL,
  reviewed_by VARCHAR(64),
  requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  reviewed_at TIMESTAMPTZ,
  decision_note VARCHAR(2048),
  lock_version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_pending_review ON reviews(comparison_id) WHERE status = 'PENDING';

CREATE TABLE evidence_snapshots (
  id BIGSERIAL PRIMARY KEY,
  review_id BIGINT NOT NULL UNIQUE REFERENCES reviews(id),
  fingerprint VARCHAR(128) NOT NULL,
  snapshot_json TEXT NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE import_jobs (
  id BIGSERIAL PRIMARY KEY,
  external_job_id VARCHAR(96) NOT NULL UNIQUE,
  status VARCHAR(24) NOT NULL CHECK (status IN ('RECEIVED','PROCESSING','COMPLETED','PARTIAL_SUCCESS','FAILED')),
  uploaded_by VARCHAR(64) NOT NULL,
  total_files INTEGER NOT NULL DEFAULT 0,
  succeeded_files INTEGER NOT NULL DEFAULT 0,
  failed_files INTEGER NOT NULL DEFAULT 0,
  error_summary VARCHAR(4000),
  claimed_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX ix_import_jobs_status ON import_jobs(status, created_at);

CREATE TABLE import_files (
  id BIGSERIAL PRIMARY KEY,
  job_id BIGINT NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
  file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(32) NOT NULL CHECK (file_type IN ('PRESSURE_CSV','PARTICLE_CSV')),
  content TEXT NOT NULL,
  status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING','PROCESSING','SUCCESS','FAILED','RETRYING')),
  attempts INTEGER NOT NULL DEFAULT 0,
  error_message VARCHAR(4000),
  checksum_sha256 VARCHAR(64) NOT NULL,
  processed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_import_file_checksum_success ON import_files(checksum_sha256) WHERE status = 'SUCCESS';
CREATE INDEX ix_import_files_job_status ON import_files(job_id, status);

ALTER TABLE particle_measurements
  ADD CONSTRAINT fk_particle_measurements_import_file
  FOREIGN KEY (import_file_id) REFERENCES import_files(id);
ALTER TABLE pressure_observations
  ADD CONSTRAINT fk_pressure_observations_import_file
  FOREIGN KEY (import_file_id) REFERENCES import_files(id);
