import { useEffect, useState } from 'react';
import { api, getCredentials, setCredentials } from './api/client';
import { Login } from './components/Login';
import { SegmentView } from './components/SegmentView';
import type { Credentials, Segment, SegmentDetail } from './types';

export default function App() {
  const [creds,setCreds]=useState<Credentials|undefined>(getCredentials());
  const [segments,setSegments]=useState<Segment[]>([]);
  const [selected,setSelected]=useState<SegmentDetail>();
  const [role,setRole]=useState('');
  const [error,setError]=useState('');
  const [busy,setBusy]=useState(false);

  async function login(c:Credentials) {
    setCredentials(c); setCreds(c); setError(''); await load();
  }
  async function load() {
    try {
      const me = await fetch('/api/me', {headers:{Authorization:`Basic ${btoa(`${creds?.username}:${creds?.password}`)}`}}).then(r=>r.json());
      setRole(me.roles?.[0] ?? '');
      setSegments(await api.segments());
    } catch(e) { setError(message(e)); }
  }
  useEffect(()=>{ if(creds) load(); },[creds?.username]);
  async function choose(id:string){ setSelected(await api.segment(id)); }
  async function action(kind:string,payload?:unknown){
    if(!selected) return;
    setBusy(true); setError('');
    try {
      const id=selected.segment.id;
      if(kind==='upload') await api.upload(id,payload as FileList);
      if(kind==='process') await api.process(id);
      if(kind==='auto') await api.autoPair(id);
      if(kind==='manual') { const p=payload as {before:string,after:string}; await api.manualPair(id,p.before,p.after); }
      if(kind==='refresh') await api.refresh(payload as string);
      if(kind==='confirm') await api.confirm(payload as string);
      if(kind==='correct') await api.correct(id,payload);
      if(kind==='lock') await api.lock(id);
      if(kind==='release') await api.release(id);
      if(kind==='approve') { const p=payload as {decision:'APPROVED'|'REJECTED',notes:string}; await api.approve(id,p.decision,p.notes); }
      await new Promise(r=>setTimeout(r,300)); await choose(id);
    } catch(e){ setError(message(e)); } finally { setBusy(false); }
  }

  if(!creds) return <Login onLogin={login}/>;
  return <main>
    <header className="top"><h1>乳品均质复核平台</h1><nav>{segments.map(s=><button key={s.id} onClick={()=>choose(s.id)}>{s.code}</button>)}
      <button onClick={()=>{setCredentials(undefined); location.reload();}}>退出 {creds.username}</button></nav></header>
    {error && <div className="error">{error}</div>}
    {busy && <div className="info">正在提交…</div>}
    {!selected && <section className="welcome"><h2>选择批段</h2><p>种子数据包含：可比、迟到前样、算法换版、分层不一致、无前样。</p>
      <p className="safe">平台只复核与展示；不推荐压力，也不控制均质机。</p></section>}
    {selected && <SegmentView detail={selected} role={role} onAction={action}/>}
  </main>;
}
function message(e:unknown){ return e instanceof Error ? e.message : String(e); }
