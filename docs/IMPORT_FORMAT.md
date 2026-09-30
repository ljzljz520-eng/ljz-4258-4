# 仪器导入格式

参考解析器读取三段落 CSV，所有字段使用 `key=value`：

```csv
sampleCode=S-1,measurementPointCode=HOM-MAIN,algorithmInstrumentCode=PSA-01,algorithmName=Fraunberger-LDD,algorithmVersion=1.8.2,sampleType=BEFORE,layer=MIDDLE,sampledAt=2026-09-30T10:03:00Z
stageOrder=1,rawLabel=P-HIGH,rawBar=150,measuredAt=2026-09-30T10:03:30Z
binUm=1.0,volumePct=8.4
```

处理语义：

1. 每文件一个独立 REQUIRES_NEW 事务；任何解析、唯一约束、外键或检查约束错误都只回滚该文件。
2. 成功文件按 SHA-256 建条件唯一约束；失败/待处理文件不占唯一键，允许修正后重试。
3. 作业状态为 `SUCCEEDED` 只有全部文件成功；否则是 `FAILED` 或 `COMPLETED_WITH_FAILURES`。
4. Worker 可恢复过期 RUNNING 租约，并重试失败文件；已成功文件不重复导入。
5. 批段 LOCKED/ISSUED 后，新事务会拒绝写入。

真实项目应在此边界实现厂商适配器、字符编码、schema 版本和数字签名校验；不要把解析失败吞成警告。
