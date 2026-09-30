import type { Reason } from '../types';
export function Reasons({ reasons }: { reasons: Reason[] }) {
  if (!reasons.length) return <div className="reasons ok">当前规则下可比；仍需实验员人工确认后才能审核。</div>;
  return <aside className="reasons"><h4>不可比/过期原因（曲线旁证据）</h4><ul>{reasons.map(r =>
    <li key={r.code}><strong>{r.code}</strong><span>{r.detail}</span></li>)}
  </ul></aside>;
}
