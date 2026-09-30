# 仪器文件样例

CSV 是三段落键值行：样品头、若干压力阶段、若干粒径 bin。上传后 Worker 为每个文件使用独立事务：

- `late-before.csv`：实际取样早、文件/到达晚，用于验证迟到前样；
- `after-new-stage.csv`：另一个后样压力阶段，可与同一个前样形成一对多候选；
- `malformed-missing-distribution.csv`：故意缺少粒径分布，必须失败并可重试，且任务状态保持部分失败。

> 生产解析器应替换为仪器厂商正式解析器和 schema；本参考实现保留算法名、算法版本和 checksum。
