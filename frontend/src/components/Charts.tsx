import type { Point, Bin } from '../types';

export function PressureChart({ before, after }: { before: Point[]; after: Point[] }) {
  const all = [...before, ...after];
  const width = 520, height = 240, pad = 42;
  const maxX = Math.max(1, ...all.map(p => p.order));
  const maxY = Math.max(1, ...all.map(p => p.correctedBar ?? p.rawBar));
  const x = (n:number) => pad + (n - 1) / Math.max(1,maxX-1) * (width - pad*2);
  const y = (v:number) => height - pad - v/maxY * (height-pad*2);
  const path = (points:Point[]) => points.map((p,i)=>`${i?'L':'M'}${x(p.order)},${y(p.correctedBar ?? p.rawBar)}`).join(' ');
  return <svg role="img" aria-label="压力阶段曲线" viewBox={`0 0 ${width} ${height}`} className="chart">
    <line x1={pad} y1={height-pad} x2={width-pad} y2={height-pad}/><line x1={pad} y1={pad} x2={pad} y2={height-pad}/>
    <path d={path(before)} className="line before"/><path d={path(after)} className="line after"/>
    {all.map((p,i)=><g key={i}><circle cx={x(p.order)} cy={y(p.correctedBar??p.rawBar)} r="3"/><text x={x(p.order)} y={height-16} textAnchor="middle">{p.canonicalLabel}</text></g>)}
    <text x={8} y={18} className="legend before">● 前样</text><text x={78} y={18} className="legend after">● 后样</text>
    <text transform={`translate(12 ${height/2}) rotate(-90)`}>bar（修正值优先）</text>
  </svg>;
}

export function DistributionChart({ before, after }: { before: Bin[]; after: Bin[] }) {
  const width = 520, height = 240, pad = 42;
  const maxY = Math.max(1, ...before.concat(after).map(b=>b.volumePct));
  const maxX = Math.max(1, ...before.concat(after).map(b=>b.binUm));
  const x = (v:number) => pad + Math.log10(v+0.05)/Math.log10(maxX+0.05) * (width-pad*2);
  const y = (v:number) => height-pad-v/maxY*(height-pad*2);
  const line = (bins:Bin[]) => bins.map((b,i)=>`${i?'L':'M'}${x(b.binUm)},${y(b.volumePct)}`).join(' ');
  const ticks = [0.1,0.2,0.5,1,2,5,10,20,50,100].filter(v=>v<=maxX);
  return <svg role="img" aria-label="粒径分布曲线" viewBox={`0 0 ${width} ${height}`} className="chart">
    <line x1={pad} y1={height-pad} x2={width-pad} y2={height-pad}/><line x1={pad} y1={pad} x2={pad} y2={height-pad}/>
    <path d={line(before)} className="line before"/><path d={line(after)} className="line after"/>
    {ticks.map(t=><text key={t} x={x(t)} y={height-16} textAnchor="middle">{t}</text>)}
    <text x={width/2-30} y={height-2}>粒径 μm（对数轴）</text><text transform={`translate(12 ${height/2}) rotate(-90)`}>体积 %</text>
    <text x={8} y={18} className="legend before">● 前样</text><text x={78} y={18} className="legend after">● 后样</text>
  </svg>;
}
