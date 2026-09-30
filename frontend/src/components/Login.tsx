import { useState } from 'react';
import type { Credentials } from '../types';
export function Login({ onLogin }:{onLogin:(c:Credentials)=>void}) {
 const [username,setUsername]=useState('lab'); const [password,setPassword]=useState('password');
 return <form className="login" onSubmit={e=>{e.preventDefault(); onLogin({username,password});}}>
  <h1>乳品均质复核</h1><p>展示、可比性检查与人工审核；不推荐压力或控制均质机。</p>
  <label>操作员/实验员/审核员 <input value={username} onChange={e=>setUsername(e.target.value)}/></label>
  <label>密码 <input type="password" value={password} onChange={e=>setPassword(e.target.value)}/></label>
  <button className="primary">登录</button><small>演示：operator / lab / reviewer，密码 password</small>
 </form>}
