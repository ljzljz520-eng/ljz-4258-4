import { useMemo, useState } from 'react';
import type { SegmentDetail } from '../types';
import { ComparisonCard } from './ComparisonCard';

export function SegmentView({ detail, role, onAction }:{
  detail: SegmentDetail; role:string;
  onAction:(kind:string, payload?:unknown)=>Promise<void>;
}) {
  const [files,setFiles]=useState<FileList>();
  const befores = detail.samples.filter(s=>s.type==='BEFORE');
  const afters = detail.samples.filter(s=>s.type==='AFTER');
  const [before,setBefore]=useState(''); const [after,setAfter]=useState('');
  const beforeSample = befores.find(s=>s.sampleCode===before); const afterSample = afters.find(s=>s.sampleCode===after);
  const nextLevel = Math.max(0,...detail.comparisons.map(c=>c.correctionLevel??0))+1;
  const [notes,setNotes]=useState('');
  const isLab = role==='ROLE_LAB_TECH'; const isReviewer = role==='ROLE_REVIEWER'; const isOperator=role==='ROLE_OPERATOR';
  const locked = detail.segment.status!=='OPEN';
  const importBad = detail.imports.status && detail.imports.status!=='SUCCEEDED' && detail.imports.status!=='NONE';
  const point = useMemo(()=>detail.samples[0], [detail]);

  return <section className="detail">
    <div className="toolbar"><div><h2>{detail.segment.code}</h2>
      <p>{detail.segment.valveGroup} · {detail.segment.samplingLine} · {detail.segment.productBatch} · 目标阶段 {detail.segment.targetStage} · <b>{detail.segment.status}</b></p>
      <small>基线 {fmt(detail.segment.baselineStart)} ~ {fmt(detail.segment.baselineEnd)}；稳定 {fmt(detail.segment.stableStart)} ~ {fmt(detail.segment.stableEnd)}</small></div>
      <div className="import"><span className={importBad?'badge bad':'badge good'}>导入：{detail.imports.status} {detail.imports.successful}/{detail.imports.total}</span>
        {isOperator && <input type="file" multiple accept=".csv,.txt" onChange={e=>setFiles(e.target.files??undefined)}/>}
        {isOperator && <button disabled={!files||locked} onClick={()=>files&&onAction('upload',files)}>上传仪器文件</button>}
        {isOperator && <button disabled={detail.imports.status==='SUCCEEDED'||detail.imports.status==='NONE'} onClick={()=>onAction('process')}>重试/立即处理</button>}
        {(isOperator||isLab) && <button onClick={()=>onAction('auto')}>扫描自动候选</button>}
      </div></div>

    {detail.imports.files.some(f=>f.status==='FAILED') && <div className="reasons"><h4>部分文件失败，不显示为完整成功</h4>
      <ul>{detail.imports.files.filter(f=>f.status==='FAILED').map(f=><li key={f.id}><strong>{f.filename}</strong><span>{f.error}</span></li>)}</ul></div>}

    {(isOperator||isLab) && <div className="manual"><label>人工改选前样<select value={before} onChange={e=>setBefore(e.target.value)}>{options(befores)}</select></label>
      <label>后样<select value={after} onChange={e=>setAfter(e.target.value)}>{options(afters)}</select></label>
      {isLab && <button disabled={!before||!after||locked} onClick={()=>onAction('manual',{before:beforeSample?.id,after:afterSample?.id})}>提出人工候选</button>}</div>}

    {isLab && <div className="correction"><h4>压力校准修正（原始标签/原始值不改）</h4>
      <button disabled={locked} onClick={()=>onAction('correct',{
        measurementPointId: point?.measurementPointId,
        rawLabel:'P-HIGH', canonicalLabel:'HIGH', correctionLevel:nextLevel, slope:1.02, intercept:-0.5,
        reason:'演示：互换复核后发现高压段传感器两点校准偏移'
      })}>将 P-HIGH 升级到修正级别 {nextLevel}（×1.02−0.5）</button><small>依赖该映射的比较会立即变为 STALE。</small></div>}

    {detail.comparisons.map(c=><ComparisonCard key={c.id} comparison={c} canConfirm={isLab && !locked}
      onRefresh={id=>onAction('refresh',id)} onConfirm={id=>onAction('confirm',id)}/>)}

    {isReviewer && <div className="review"><h4>审核与证据快照</h4>
      {detail.review?.sha256 && <small title={detail.review.sha256}>快照 {detail.review.sha256.slice(0,26)}…</small>}
      <textarea placeholder="审核备注" value={notes} onChange={e=>setNotes(e.target.value)}/>
      <button disabled={locked} onClick={()=>onAction('lock')}>锁定证据快照</button>
      <button disabled={detail.segment.status!=='LOCKED'} onClick={()=>onAction('approve',{decision:'APPROVED',notes})}>签发批准</button>
      <button disabled={detail.segment.status!=='LOCKED'} onClick={()=>onAction('approve',{decision:'REJECTED',notes})}>签发驳回</button>
      <button disabled={detail.segment.status!=='LOCKED'} onClick={()=>onAction('release')}>释放锁定</button>
      <p>锁定时会重新生成证据 SHA-256；签发前再次校验，防止导入途中签发。</p></div>}
  </section>;
}
function options(list:{sampleCode:string}[]) { return <><option value="">选择样品</option>{list.map(s=><option key={s.sampleCode} value={s.sampleCode}>{s.sampleCode}</option>)}</>; }
function fmt(v:string){return new Date(v).toLocaleString()}
