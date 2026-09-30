import React from 'react';
export function IncomparableReasons({findings=[]}) {
  const blockers=findings.filter(f=>f.severity==='BLOCKER');
  return <aside className={`reasons ${blockers.length?'blocked':'clear'}`}><h3>曲线旁不可比原因</h3>{!blockers.length && <p className="ok">强制检查通过；仍需实验员人工确认后送审。</p>}<ul>{blockers.map((f,i)=><li key={i}><b>{f.message}</b><span>{f.detail}</span><code>{f.code}</code></li>)}</ul><p className="boundary">自动系统只给出候选和规则；人工不能覆盖管线、产品批、分层、时序、稳定窗口、压力映射版本、粒度算法版本或导入完整性。</p></aside>;
}
