import React from 'react';
export function DistributionChart({title='粒径分布', curve}) {
  if (!curve) return <div className="chart empty"><h3>{title}</h3><p>缺少样品证据</p></div>;
  const points = curve.distribution || [];
  const width=360,height=220,pad=34;
  const maxX=Math.max(...points.map(p=>Number(p.binSizeUm)),1);
  const maxY=Math.max(...points.map(p=>Number(p.volumeFraction)),1);
  const path=points.map((p,i)=>`${i?'L':'M'} ${pad+(p.binSizeUm/maxX)*(width-pad*2)} ${height-pad-(p.volumeFraction/maxY)*(height-pad*2)}`).join(' ');
  return <div className="chart"><h3>{title} · {curve.sampleCode}</h3><p>原始压力 {curve.rawPressureLabel||'—'} → 校准阶段 {curve.calibratedStageCode||'—'}；算法 {curve.algorithmVersion||'—'}</p>
    <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="粒径体积分数曲线"><line x1={pad} y1={height-pad} x2={width-pad} y2={height-pad}/><line x1={pad} y1={pad} x2={pad} y2={height-pad}/><path d={path}/><text x={pad} y={height-8}>粒径 μm</text><text x="8" y={pad}>体积分数</text></svg>
    <small>D10 {curve.d10Um ?? '—'} / D50 {curve.d50Um ?? '—'} / D90 {curve.d90Um ?? '—'}</small>
  </div>;
}
