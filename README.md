# 乳品均质复核全栈平台

平台用于**展示和复核**乳品均质前后的压力阶段与粒径分布。它不会推荐均质压力，也不会向均质机或任何控制系统发出设定值。

## 组件

- `frontend/`：React + TypeScript + Vite，展示压力阶段、粒径分布曲线、不可比原因、证据快照与审核状态。
- `backend/`：Spring Boot 3 + Spring Security，负责批段、候选配对、人工确认、压力修正、证据锁定与审核。
- PostgreSQL 16：保存阀组、测点、样品、原始压力标签、校准映射、仪器算法版本、导入任务、比较和证据快照。
- 后端内置 Worker：定时拉取导入任务，逐文件独立事务导入；失败可重试，部分失败绝不会显示为完整成功。
- Flyway：交付数据库迁移与样例主数据。

## 角色边界

| 角色 | 典型权限 |
| --- | --- |
| `operator` 操作员 | 上传/重试仪器文件、查看数据、触发自动候选扫描 |
| `lab_tech` 实验员 | 查看、人工选择/刷新/确认候选对、维护压力校准修正映射 |
| `reviewer` 审核员 | 锁定证据快照、签发审核；不能导入或确认候选 |

服务端使用方法级授权，前端控制仅为交互辅助。

## 自动配对与人工确认边界

系统自动执行：

1. 根据样品实际 `sampledAt` 而不是到达时间扫描前后样，因此允许“前样晚到”。
2. 为一个前样提出多个后样压力阶段候选，即一对多候选。
3. 逐项检查取样管线、产品批、样品层级、稳定/基线窗口、粒径算法版本、压力映射和导入任务状态。
4. 将阻断原因显示在对应曲线旁。

人工负责：

- 实验员确认候选；存在阻断原因的候选不能确认。
- 实验员可改选候选，系统会把旧候选标记为 `SUPERSEDED`，但不会绕过可比性规则。
- 审核员锁定不可变证据快照并签发。

系统不会自动确认、自动审核、推荐压力或控制均质机。

## 本地运行

```bash
docker compose up --build
# API: http://localhost:8080
# UI:  http://localhost:5173
```

演示账号密码均为 `password`：`operator`、`lab`、`reviewer`。

手动运行：

```bash
cd backend
mvn spring-boot:run

cd frontend
npm install
npm run dev
```

## 测试

```bash
cd backend && mvn verify
cd frontend && npm install && npm test -- --run && npm run build
```

后端集成测试使用 Testcontainers PostgreSQL，需要本地 Docker。本次工作区已完成：

- 前端 `npm test -- --run` 与 `npm run build`；
- 后端 `mvn -DskipTests package` 编译与测试源码编译；
- 后端 Testcontainers 套件在此无 Docker 环境中未执行，需在 Docker/CI 中运行 `mvn verify`。

验证矩阵见 [docs/VALIDATION.md](docs/VALIDATION.md)。更多设计见
[docs/OPERATING_MODEL.md](docs/OPERATING_MODEL.md)、
[docs/DATA_MODEL.md](docs/DATA_MODEL.md)、
[docs/API.md](docs/API.md)、
[docs/IMPORT_FORMAT.md](docs/IMPORT_FORMAT.md)。
