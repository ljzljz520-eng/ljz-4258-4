import React from 'react';
export function PressureStageChart({points}) {
  if(!points?.length) return <div className="chart empty"><h3>压力阶段</h3><p>尚无压力文件</p></div>;
  const max=Math.max(...points.map(p=>Number(p.observedPressureBar||0)),1);
  return <div className="chart"><h3>压力原始标签 / 校准映射</h3><div className="bars">{points.map((p,i)=><div key={i} className="bar"><span>{p.rawLabel} → {p.calibratedStageCode}</span><div style={{width:`${Math.max(8,Number(p.observedPressureBar)/max*100)}%`}}>{p.observedPressureBar} bar</div></div>)}</div></div>;
}
