# 乳品均质证据复核全栈平台

面向乳品均质前后样的**证据复核**平台：展示压力阶段和粒径分布，管理批段、自动/人工候选配对、算法/校准版本、审核证据快照和仪器文件导入。

> 安全边界：系统不推荐压力设定值，不向均质机或任何 PLC/SCADA 下发控制指令；所有结论仅用于质量证据复核。

## 技术栈

- React 18 + Vite：压力阶段条、粒径 SVG 曲线、曲线旁阻断原因、候选/审核操作。
- Spring Boot 3：REST、RBAC、配对规则、校准修正、审核快照、Worker 调度。
- PostgreSQL 16 + Flyway：阀组、取样点、批、稳定窗口、原始标签/校准映射版本、样品、粒度测量、比较、审核和导入证据。
- Worker：同一 Spring Boot 应用以 `worker` profile 运行，轮询导入任务；每个仪器文件一个独立事务，可单文件重试。

## 角色权限

| 角色 | 主要权限 |
|---|---|
| `OPERATOR` 操作员 | 上传/重试仪器文件、维护基础生产批次/取样点、读取复核信息 |
| `LAB_ANALYST` 实验员 | 登记算法版本和稳定窗口、修正压力校准映射、自动/手工配对、确认/退回、送审 |
| `REVIEWER` 审核员 | 批准/退回；批准时写入不可变证据快照并锁定比较 |

演示账号密码均为 `password`：`operator`、`analyst`、`reviewer`。

## 快速启动

```bash
docker compose up --build
# API: http://localhost:8080
# Web: http://localhost:5173
```

本地开发：

```bash
# 1. PostgreSQL
docker compose up postgres

# 2. API（自动执行 Flyway）
cd backend
mvn spring-boot:run

# 3. Worker（另一个终端）
mvn spring-boot:run -Dspring-boot.run.profiles=worker

# 4. Web
cd frontend
npm install
npm run dev
```

## 自动配对与人工确认边界

自动配对只创建候选，不做签发：

1. 同产品批、同阀组、同取样管线、同分层；
2. 前样/后样角色正确，运输绝对时间差不超过配置窗口（默认 1440 分钟）；
3. 一个前样可匹配多个后样，一个后样也可保留多个候选，界面显式列出一对多关系；
4. 即使出现“迟到前样”（后样采样时间早于前样），也保留候选并显示 `LATE_BEFORE_SAMPLE`，而不是静默丢弃；
5. 自动候选仍必须通过全部强制规则并由实验员人工确认后才能送审。

人工可以：指定前样/后样建候选、确认、退回、送审、填写审核意见。

人工**不能覆盖**：取样管线、产品批、样品分层、前后时序、运输时延、稳定窗口、压力阶段解析、压力校准映射版本、粒度仪器算法版本、测量完整性、导入任务完整性。

## 比较前强制证据检查

规则实现在 `ComparabilityEngine`，阻断原因随比较结果返回并显示在两条曲线旁：

- `SAMPLING_LINE_MISMATCH`：取样管线不同；
- `BATCH_MISMATCH`：产品批不同；
- `LAYER_MISMATCH`：样品分层不同；
- `LATE_BEFORE_SAMPLE` / `TRANSPORT_DELAY_EXCEEDED`：时序倒挂或运输时延超窗；
- `STABLE_WINDOW_MISS` / `STAGE_WINDOW_MISMATCH`：后样不在稳定窗口或阶段不一致；
- `PRESSURE_STAGE_UNRESOLVED` / `PRESSURE_STAGE_MISMATCH`：原始标签无法校准或阶段不同；
- `PRESSURE_MAPPING_VERSION_MISMATCH`：前后样依赖不同校准版本；
- `PARTICLE_ALGORITHM_VERSION_MISMATCH`：粒度算法版本不同；
- `MEASUREMENT_MISSING`：缺少有效粒度测量；
- `EVIDENCE_IMPORT_INCOMPLETE`：关联文件所在导入任务仍处理中、失败或部分成功。

## 压力标签和校准映射

`pressure_observations.raw_label` 永久保留仪器原始标签；校准结果通过带版本的 `pressure_mapping_versions/items` 映射。修正映射时：

1. 阀组行级悲观锁定，阻塞与审核/导入并发交叉；
2. 旧版本置 `SUPERSEDED` 并写入 `effective_to`；
3. 新版本生效后重刷历史观测的解析阶段；
4. 未批准且依赖旧映射的候选/已确认比较置 `EXPIRED` 并重新评估；
5. 已 `APPROVED` 的比较及其证据快照保持锁定，不被后台静默改写。

## 导入原子性与重试

- 任务状态：`RECEIVED`、`PROCESSING`、`COMPLETED`、`PARTIAL_SUCCESS`、`FAILED`；
- 文件状态：`PENDING`、`PROCESSING`、`SUCCESS`、`FAILED`、`RETRYING`；
- 每个文件使用独立事务，单文件失败只回滚该文件，不污染其他文件；
- 失败文件可调用 `POST /api/imports/files/{id}/retry` 重试；
- 任一文件未成功时任务是 `PARTIAL_SUCCESS`，相关比较保持 `EVIDENCE_IMPORT_INCOMPLETE`，禁止送审/签发；
- 文件按 SHA-256 幂等去重，成功文件不得重复导入形成“新证据”；
- 接收或处理中的全局导入任务存在时，禁止审核签发和压力级别映射修正。

## 典型 API

- `POST /api/imports`：multipart 上传，字段 `files`、`type=PRESSURE_CSV|PARTICLE_CSV`；
- `POST /api/imports/process-next`：测试/本地无 worker 时立即处理；
- `POST /api/imports/files/{id}/retry`：重试单文件；
- `POST /api/comparisons/auto-pair/batches/{batchId}`：生成候选并重算规则；
- `POST /api/comparisons/manual`：人工建候选；
- `POST /api/comparisons/{id}/confirm|reject|recheck`；
- `POST /api/pressure-mappings/correct`：实验员修正校准；
- `POST /api/reviews/comparisons/{id}/request`；
- `POST /api/reviews/comparisons/{id}/decision`：仅审核员。

## 样例与测试

样例 CSV 位于 `samples/`。先导入压力，再导入粒度。

```bash
# 后端：14 个测试（纯规则、真实 PostgreSQL Flyway 迁移、Spring 工作流、权限）
cd backend && mvn test

# 前端：3 个组件/规则展示测试
cd frontend && npm test && npm run build
```

测试覆盖：Flyway 可在真实 PostgreSQL 执行、压力标签互换后未锁定比较过期/已批准快照不变、粒度算法换版阻断、一对多候选与样品分层、迟到前样保留并阻断、部分失败后单文件重试、并发审核/修正、RBAC。
