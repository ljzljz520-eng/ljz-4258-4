# 数据模型重点

- `pressure_readings.raw_label/raw_value_bar` 是仪器原始证据，永不覆盖。
- `pressure_calibration_mappings` 保存版本化的标签到规范阶段映射、斜率/截距和修正级别；修正时旧映射置 inactive，新增版本。
- `pressure_readings.mapping_id/corrected_value_bar` 表示解释版本；`comparison_dependencies` 固定比较所依赖的压力映射和粒度算法版本。
- `instrument_algorithm_versions` 独立记录仪器、算法名、版本和 checksum。
- `samples.sampled_at` 是实际取样时间；`received_at` 是文件/样品到达时间。自动配对只以前者确定前后，因此支持迟到前样。
- `import_files` 每文件独立 checksum 和状态；成功文件有条件唯一索引，失败文件可替换内容重试。
- `evidence_snapshots.payload/sha256` 锁定证据；`reviews` 保存锁证和签发决策。
