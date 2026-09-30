INSERT INTO import_jobs (id, segment_id, status, total_files, successful_files, failed_files, created_by, created_at, started_at, finished_at)
SELECT gen_random_uuid(), id, 'SUCCEEDED', 1, 1, 0, 'migration', '2026-09-30 09:55:00+00', '2026-09-30 09:55:00+00', '2026-09-30 09:56:00+00'
FROM batch_segments;

INSERT INTO import_files (id, job_id, filename, checksum, content, status, attempts, processed_at, created_at)
SELECT gen_random_uuid(), j.id, concat(s.segment_code,'-seed.txt'), concat('sha256:seed-', s.segment_code),
       convert_to(concat('seed evidence for ', s.segment_code), 'UTF8'), 'SUCCEEDED', 1, '2026-09-30 09:56:00+00', '2026-09-30 09:55:00+00'
FROM (
  SELECT bs.id AS segment_id, bs.code AS segment_code FROM batch_segments bs
) s
JOIN import_jobs j ON j.segment_id = s.segment_id;

UPDATE samples sa
SET import_file_id = f.id
FROM import_jobs j
JOIN import_files f ON f.job_id = j.id
WHERE j.segment_id = sa.segment_id AND sa.import_file_id IS NULL;
