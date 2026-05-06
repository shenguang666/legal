import { apiDelete, apiGet, apiPost, apiPostSse, apiPut } from './client';
export function listCourtCases(pageNo = 1, pageSize = 10) {
    return apiGet(`/api/smart-court/cases?pageNo=${pageNo}&pageSize=${pageSize}`);
}
export function createCourtCase(payload) {
    return apiPost('/api/smart-court/cases', payload);
}
export function updateCourtCase(caseId, payload) {
    return apiPut(`/api/smart-court/cases/${caseId}`, payload);
}
export function confirmCourtFacts(caseId) {
    return apiPost(`/api/smart-court/cases/${caseId}/confirm-facts`);
}
export function startCourtHearing(caseId) {
    return apiPost(`/api/smart-court/cases/${caseId}/start-hearing`);
}
export function streamCourtHearing(caseId, payload, onEvent) {
    return apiPostSse(`/api/smart-court/cases/${caseId}/hearing-stream`, payload, ({ name, data }) => {
        onEvent(name, data);
    });
}
export function listCourtHearingRecords(caseId) {
    return apiGet(`/api/smart-court/cases/${caseId}/hearing-records`);
}
export function deleteCourtCase(caseId) {
    return apiDelete(`/api/smart-court/cases/${caseId}`);
}
export function getCourtGraph(caseId, params = {}) {
    const search = new URLSearchParams();
    if (params.focusClaimId)
        search.set('focusClaimId', params.focusClaimId);
    if (params.hops)
        search.set('hops', String(params.hops));
    if (params.limit)
        search.set('limit', String(params.limit));
    const query = search.toString();
    return apiGet(`/api/smart-court/cases/${caseId}/graph${query ? `?${query}` : ''}`);
}
export function listCourtSuggestions(caseId, status) {
    return apiGet(`/api/smart-court/cases/${caseId}/suggestions${status ? `?status=${status}` : ''}`);
}
export function ignoreCourtSuggestion(caseId, suggestionId) {
    return apiPost(`/api/smart-court/cases/${caseId}/suggestions/${suggestionId}/ignore`);
}
export function resolveCourtSuggestion(caseId, suggestionId) {
    return apiPost(`/api/smart-court/cases/${caseId}/suggestions/${suggestionId}/resolve`);
}
export function generateCourtReport(caseId, roundId = null, judgeOutput) {
    return apiPost(`/api/smart-court/cases/${caseId}/judgment-report`, { roundId, judgeOutput });
}
export function auditCourtReportExport(caseId, exportFormat) {
    return apiPost(`/api/smart-court/cases/${caseId}/judgment-report/export-audit?exportFormat=${encodeURIComponent(exportFormat)}`);
}
