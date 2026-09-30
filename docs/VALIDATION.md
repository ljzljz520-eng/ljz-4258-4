# 验证矩阵

| 场景 | 自动化测试 | 期望结果 |
| --- | --- | --- |
| 压力互换/校准修正 | `pressureInterchange...` | 原始标签和原始值不变；生成新映射版本；依赖比较变 `STALE`，刷新并重新确认前不能进快照 |
| 粒度算法换版 | `particleAlgorithmVersionChange...` | 前后样版本不同，候选旁显示 `PARTICLE_ALGORITHM_MISMATCH`，不能确认 |
| 样品分层 | `sampleLayering...` | 顶层/中层等层级不同，显示 `LAYER_MISMATCH`，不能确认 |
| 迟到前样 | `lateBeforeSample...` | 后样先到也可暂存；前样到达后按实际取样时间自动形成候选 |
| 部分导入失败 | `partialFileFailure...` | 任务为 `COMPLETED_WITH_FAILURES`；成功文件可见，失败文件保留错误并可重试；审核锁定被阻止 |
| 并发审核/修正 | `concurrentReviewAndCorrection...` | 修正先提交则审核因 stale 失败；审核先提交则快照不可变，后续修正只影响后续复核；重复签发遇到乐观锁冲突 |
| 角色隔离 | `SecurityIntegrationTest` | 操作员不能审核，实验员不能签发，审核员不能导入 |

## 关键不变量

1. 压力原始标签/原始值与校准映射分表保存，修正只写新映射和修正值，不改原始证据。
2. 比较依赖压力映射 ID 与粒径算法版本 ID。
3. 证据快照 payload 带 SHA-256 指纹；审核时重新计算当前证据指纹，不一致即拒绝。
4. `RUNNING/PENDING/FAILED/COMPLETED_WITH_FAILURES` 导入任务均阻止签发。
5. 锁定或已签发批段拒绝可改变证据的导入，避免导入途中签发。
6. 系统不提供压力推荐接口，也没有任何均质机控制出口。
