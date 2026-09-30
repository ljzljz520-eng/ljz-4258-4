import type { Credentials, Segment, SegmentDetail } from '../types';

const BASE = '/api';
let credentials: Credentials | undefined;
export function setCredentials(value: Credentials | undefined) { credentials = value; }
export function getCredentials() { return credentials; }

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (credentials) headers.set('Authorization', 'Basic ' + btoa(`${credentials.username}:${credentials.password}`));
  if (!(init.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  const response = await fetch(BASE + path, { ...init, headers });
  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    try { message = (await response.json()).error || message; } catch {}
    throw new Error(message);
  }
  return response.status === 204 ? undefined as T : response.json();
}

export const api = {
  segments: () => request<Segment[]>('/segments'),
  segment: (id: string) => request<SegmentDetail>(`/segments/${id}`),
  autoPair: (id: string) => request(`/segments/${id}/auto-pair`, { method: 'POST' }),
  manualPair: (id:string, beforeSampleId:string, afterSampleId:string) =>
    request(`/segments/${id}/comparisons`, { method:'POST', body: JSON.stringify({beforeSampleId, afterSampleId})}),
  confirm: (id:string) => request(`/comparisons/${id}/confirm`, { method:'POST' }),
  refresh: (id:string) => request(`/comparisons/${id}/refresh`, { method:'POST' }),
  correct: (segmentId:string, body:unknown) => request(`/segments/${segmentId}/pressure-mappings/corrections`, { method:'POST', body: JSON.stringify(body)}),
  lock: (id:string) => request(`/segments/${id}/review/lock`, {method:'POST'}),
  release: (id:string) => request(`/segments/${id}/review/release`, {method:'POST'}),
  approve: (id:string, decision:'APPROVED'|'REJECTED', notes:string) =>
    request(`/segments/${id}/review/decision`, {method:'POST', body:JSON.stringify({decision, notes})}),
  upload: (id:string, files:FileList) => {
    const body = new FormData(); Array.from(files).forEach(f => body.append('files', f));
    return request(`/segments/${id}/imports`, { method:'POST', body });
  },
  process: (id:string) => request(`/segments/${id}/imports/process`, {method:'POST'})
};
