# 样例仪器文件

- `pressure-stage-observations.csv`：压力仪器导出。`raw_label` 是仪器原始标签，校准阶段只来自服务端 `pressure_mapping_versions/items`。
- `particle-size-candidates.csv`：一个前样对两个同层后样，形成一对多候选，人工选择确认后才能送审。
- `particle-size-bad-layer-or-algorithm.csv`：分层异常样例。手工建候选后会在曲线旁显示 `LAYER_MISMATCH`。

导入顺序：先压力文件，再粒度文件。单文件成功不代表任务完整；任一文件失败时任务为 `PARTIAL_SUCCESS`，相关比较保持不可送审。
