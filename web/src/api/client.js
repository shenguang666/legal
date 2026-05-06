const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL;
const normalizedApiBaseUrl = configuredApiBaseUrl === undefined ? 'http://localhost:8081' : configuredApiBaseUrl.trim().replace(/\/+$/, '');
const API_BASE_URL = normalizedApiBaseUrl === '.' ? '' : normalizedApiBaseUrl;
function getToken() {
    return localStorage.getItem('legal.token') || '';
}
function authHeaders() {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
}
export function saveAuth(user) {
    localStorage.setItem('legal.token', user.token || '');
    localStorage.setItem('legal.tenantId', String(user.tenantId));
    localStorage.setItem('legal.userId', String(user.userId));
    localStorage.setItem('legal.username', user.username || '');
    localStorage.setItem('legal.displayName', user.displayName || '');
    localStorage.setItem('legal.role', user.roleCode || 'USER');
}
export function clearAuth() {
    localStorage.removeItem('legal.token');
    localStorage.removeItem('legal.tenantId');
    localStorage.removeItem('legal.userId');
    localStorage.removeItem('legal.username');
    localStorage.removeItem('legal.displayName');
    localStorage.removeItem('legal.role');
    localStorage.removeItem('legal.activeSessionId');
}
export function hasToken() {
    return !!getToken();
}
export function currentRole() {
    return localStorage.getItem('legal.role') || 'USER';
}
async function request(path, options = {}) {
    const headers = {};
    if (!options.formData) {
        headers['Content-Type'] = 'application/json';
    }
    if (options.auth !== false) {
        Object.assign(headers, authHeaders());
    }
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: options.method || 'GET',
        headers,
        body: options.body
            ? options.formData
                ? options.body
                : JSON.stringify(options.body)
            : undefined,
    });
    const data = (await response.json());
    if (response.status === 401) {
        clearAuth();
    }
    if (!response.ok || data.code !== 0) {
        throw new Error(data?.message || `请求失败(${response.status})`);
    }
    return data.data;
}
export function apiGet(path) {
    return request(path, { method: 'GET' });
}
export function apiPost(path, body, auth = true) {
    return request(path, { method: 'POST', body, auth });
}
export function apiPut(path, body) {
    return request(path, { method: 'PUT', body });
}
export function apiPostForm(path, formData, auth = true) {
    return request(path, { method: 'POST', body: formData, auth, formData: true });
}
export function apiDelete(path) {
    return request(path, { method: 'DELETE' });
}
// fetch + ReadableStream 解析 SSE（用于 POST /api/chat/ask/stream）
export function apiPostSse(path, payload, onEvent, auth = true) {
    const controller = new AbortController();
    const headers = { 'Content-Type': 'application/json' };
    if (auth) {
        Object.assign(headers, authHeaders());
    }
    fetch(`${API_BASE_URL}${path}`, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal,
    })
        .then(async (resp) => {
        if (!resp.ok || !resp.body) {
            throw new Error(`SSE 请求失败: ${resp.status}`);
        }
        const reader = resp.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';
        let currentEvent = 'message';
        while (true) {
            const { done, value } = await reader.read();
            if (done)
                break;
            buffer += decoder.decode(value, { stream: true });
            let idx;
            while ((idx = buffer.indexOf('\n')) >= 0) {
                const line = buffer.slice(0, idx).trimEnd();
                buffer = buffer.slice(idx + 1);
                if (!line)
                    continue;
                if (line.startsWith('event:')) {
                    currentEvent = line.slice('event:'.length).trim();
                    continue;
                }
                if (line.startsWith('data:')) {
                    const raw = line.slice('data:'.length).trim();
                    let data = raw;
                    try {
                        data = JSON.parse(raw);
                    }
                    catch {
                        // ignore
                    }
                    onEvent({ name: currentEvent, data });
                }
            }
        }
    })
        .catch((err) => {
        onEvent({ name: 'error', data: { message: err?.message || String(err) } });
    });
    return () => controller.abort();
}
// fetch + ReadableStream 解析 GET SSE（用于需要 Authorization 请求头的流式接口）
export function apiGetSse(path, onEvent, auth = true) {
    const controller = new AbortController();
    const headers = {};
    if (auth) {
        Object.assign(headers, authHeaders());
    }
    fetch(`${API_BASE_URL}${path}`, {
        method: 'GET',
        headers,
        signal: controller.signal,
    })
        .then(async (resp) => {
        if (!resp.ok || !resp.body) {
            throw new Error(`SSE 请求失败: ${resp.status}`);
        }
        const reader = resp.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';
        let currentEvent = 'message';
        while (true) {
            const { done, value } = await reader.read();
            if (done)
                break;
            buffer += decoder.decode(value, { stream: true });
            let idx;
            while ((idx = buffer.indexOf('\n')) >= 0) {
                const line = buffer.slice(0, idx).trimEnd();
                buffer = buffer.slice(idx + 1);
                if (!line)
                    continue;
                if (line.startsWith('event:')) {
                    currentEvent = line.slice('event:'.length).trim();
                    continue;
                }
                if (line.startsWith('data:')) {
                    const raw = line.slice('data:'.length).trim();
                    let data = raw;
                    try {
                        data = JSON.parse(raw);
                    }
                    catch {
                        // ignore
                    }
                    onEvent({ name: currentEvent, data });
                }
            }
        }
    })
        .catch((err) => {
        onEvent({ name: 'error', data: { message: err?.message || String(err) } });
    });
    return () => controller.abort();
}
export async function apiLogin(payload) {
    const user = await apiPost('/api/auth/login', payload, false);
    saveAuth(user);
    return user;
}
export async function apiLogout() {
    try {
        await apiPost('/api/auth/logout');
    }
    finally {
        clearAuth();
    }
}
export function apiGetMe() {
    return apiGet('/api/auth/me');
}
export function randomRequestId(prefix) {
    const rand = Math.random().toString(36).slice(2, 10);
    return `${prefix}-${Date.now()}-${rand}`;
}
