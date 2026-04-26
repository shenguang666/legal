const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';
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
    const headers = {
        'Content-Type': 'application/json',
    };
    if (options.auth !== false) {
        Object.assign(headers, authHeaders());
    }
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: options.method || 'GET',
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined,
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
export function apiDelete(path) {
    return request(path, { method: 'DELETE' });
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
