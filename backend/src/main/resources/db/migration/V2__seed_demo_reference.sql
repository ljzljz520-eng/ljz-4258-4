INSERT INTO users (id, username, full_name, role, password_hash) VALUES
 (1,'operator','生产线操作员','OPERATOR','$2a$10$EuzFtC7RikZ6otB.0TK6pOmiNDswk./vCyM0AdGEo85heBE5xs.hC'),
 (2,'analyst','理化实验员','LAB_ANALYST','$2a$10$EuzFtC7RikZ6otB.0TK6pOmiNDswk./vCyM0AdGEo85heBE5xs.hC'),
 (3,'reviewer','质量审核员','REVIEWER','$2a$10$EuzFtC7RikZ6otB.0TK6pOmiNDswk./vCyM0AdGEo85heBE5xs.hC');

INSERT INTO valve_groups (id, code, name, description) VALUES
 (1,'VG-A','一号均质阀组','演示用前后取样阀组');

INSERT INTO sample_points (id, code, name, valve_group_id, sampling_line, product_flow_order) VALUES
 (1,'LINE-A-BEFORE','A线均质前取样点',1,'LINE-A',10),
 (2,'LINE-A-AFTER','A线均质后取样点',1,'LINE-A',20);

INSERT INTO product_batches (id, batch_number, product_code, product_name, layer_name, started_at, ended_at, status) VALUES
 (1,'B-20260930-MILK-A','WMP-35','全脂巴氏乳','BULK','2026-09-30 08:30:00+00','2026-09-30 11:00:00+00','OPEN');

INSERT INTO pressure_mapping_versions
 (id, valve_group_id, version, status, effective_from, corrected_by, corrected_at, change_summary) VALUES
 (1,1,1,'ACTIVE','2026-09-30 00:00:00+00',NULL,NULL,'仪器初装校准：P1/P2 分别映射到 180/220 bar 阶段');

INSERT INTO pressure_mapping_items (version_id, raw_label, calibrated_stage_code, nominal_pressure_bar, display_order) VALUES
 (1,'P1','STAGE_180',180,10),
 (1,'P2','STAGE_220',220,20);

INSERT INTO stability_windows (id, batch_id, valve_group_id, stage_code, started_at, ended_at, nominal_pressure_bar, notes) VALUES
 (1,1,1,'STAGE_180','2026-09-30 09:00:00+00','2026-09-30 09:30:00+00',180,'低压稳定窗口'),
 (2,1,1,'STAGE_220','2026-09-30 09:30:00+00','2026-09-30 10:00:00+00',220,'高压稳定窗口');

INSERT INTO instrument_algorithm_versions
 (id, instrument_code, measurement_kind, algorithm_version, status, effective_from, description) VALUES
 (1,'PSA-900','PARTICLE_SIZE','PSD-1.0','ACTIVE','2026-01-01 00:00:00+00','基线米氏散射算法'),
 (2,'PSA-900','PARTICLE_SIZE','PSD-2.0','INACTIVE','2026-10-01 00:00:00+00','换版后的折射率与去背景算法，仅供换版测试');

SELECT setval(pg_get_serial_sequence('users','id'), 3);
SELECT setval(pg_get_serial_sequence('valve_groups','id'), 1);
SELECT setval(pg_get_serial_sequence('sample_points','id'), 2);
SELECT setval(pg_get_serial_sequence('product_batches','id'), 1);
SELECT setval(pg_get_serial_sequence('pressure_mapping_versions','id'), 1);
SELECT setval(pg_get_serial_sequence('stability_windows','id'), 2);
SELECT setval(pg_get_serial_sequence('instrument_algorithm_versions','id'), 2);
