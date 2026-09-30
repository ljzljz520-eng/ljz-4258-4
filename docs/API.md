# API 摘要与权限

除 `/actuator/health` 外均使用 HTTP Basic，角色：`ROLE_OPERATOR`、`ROLE_LAB_TECH`、`ROLE_REVIEWER`。

| 方法和路径 | 角色 | 说明 |
| --- | --- | --- |
| `GET /api/me` | 任意登录 | 当前账号与角色 |
| `GET /api/segments` | 任意登录 | 批段列表 |
| `GET /api/segments/{id}` | 任意登录 | 样品、曲线、候选、原因、导入和审核摘要 |
| `POST /api/segments/{id}/imports` | 操作员 | multipart `files` 多文件上传 |
| `POST /api/segments/{id}/imports/process` | 操作员 | 测试/无调度部署时立即处理任务 |
| `POST /api/segments/{id}/auto-pair` | 操作员、实验员 | 按实际取样时间自动扫描候选 |
| `POST /api/segments/{id}/comparisons` | 实验员 | 人工选择前后样；仍服务端复算可比性 |
| `POST /api/comparisons/{id}/refresh` | 实验员 | 映射换版/STALE 后刷新依赖和原因 |
| `POST /api/comparisons/{id}/confirm` | 实验员 | 无可比阻断原因才允许确认 |
| `POST /api/segments/{id}/pressure-mappings/corrections` | 实验员 | 新压力映射版本；不改原始标签/原始值 |
| `POST /api/segments/{id}/review/lock` | 审核员 | 创建/锁定证据快照 |
| `POST /api/segments/{id}/review/decision` | 审核员 | 批准/驳回，签发前校验快照指纹 |
| `POST /api/segments/{id}/review/release` | 审核员 | 释放未签发锁定 |

没有压力推荐端点，也没有向均质机写设定值的客户端或服务端组件。

## 不可比原因码

- `IMPORT_NOT_COMPLETE` / `IMPORT_PARTIAL_FAILURE` / `IMPORT_FAILED`
- `SAMPLING_LINE_MISMATCH`
- `PRODUCT_BATCH_MISMATCH`
- `LAYER_MISMATCH`
- `BEFORE_OUTSIDE_BASELINE_WINDOW`
- `AFTER_OUTSIDE_STABLE_WINDOW`
- `BEFORE_NOT_EARLIER_THAN_AFTER`
- `PARTICLE_ALGORITHM_MISMATCH`
- `PRESSURE_MAPPING_MISSING` / `PRESSURE_MAPPING_MISMATCH` / `PRESSURE_STAGE_MISMATCH`
- `PRESSURE_MAPPING_INCONSISTENT` / `PRESSURE_MAPPING_CHANGED`
