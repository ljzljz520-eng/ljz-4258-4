import React from 'react';
import {describe, it, expect} from 'vitest';
import {render, screen} from '@testing-library/react';
import {IncomparableReasons} from '../components/IncomparableReasons.jsx';
import {DistributionChart} from '../components/DistributionChart.jsx';

describe('不可比原因展示', () => {
  it('在曲线旁列出阻断原因和代码', () => {
    render(<IncomparableReasons findings={[
      {code:'PARTICLE_ALGORITHM_VERSION_MISMATCH', severity:'BLOCKER', message:'粒度仪器算法版本不一致', detail:'PSD-1.0 vs PSD-2.0'},
      {code:'COMPARABLE', severity:'PASS', message:'通过'}
    ]}/>);
    expect(screen.getByText('粒度仪器算法版本不一致')).toBeTruthy();
    expect(screen.getByText('PSD-1.0 vs PSD-2.0')).toBeTruthy();
    expect(screen.getByText('PARTICLE_ALGORITHM_VERSION_MISMATCH')).toBeTruthy();
  });
  it('无阻断时提示仍需人工确认', () => {
    render(<IncomparableReasons findings={[{code:'COMPARABLE',severity:'PASS'}]}/>);
    expect(screen.getByText(/强制检查通过/)).toBeTruthy();
  });
});

describe('粒径分布曲线', () => {
  it('展示算法版本和校准后的压力阶段', () => {
    render(<DistributionChart curve={{sampleCode:'A1',rawPressureLabel:'P1',calibratedStageCode:'STAGE_180',algorithmVersion:'PSD-1.0',d10Um:1,d50Um:2,d90Um:4,distribution:[{binSizeUm:1,volumeFraction:.2},{binSizeUm:2,volumeFraction:.8}]}}/>);
    expect(screen.getByText(/P1/)).toBeTruthy();
    expect(screen.getByText(/STAGE_180/)).toBeTruthy();
    expect(screen.getByText(/PSD-1.0/)).toBeTruthy();
  });
});
