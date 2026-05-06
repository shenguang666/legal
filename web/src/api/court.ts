import { apiDelete, apiGet, apiPost, apiPut } from './client';

export interface CourtCase {
  caseId: number;
  tenantId: number;
  ownerUserId: number;
  title: string;
  userSide: 'PLAINTIFF' | 'DEFENDANT';
  status: string;
  caseSummary?: string;
  userObjective?: string;
  factsConfirmed?: boolean;
  totalRounds?: number;
  totalTokens?: number;
  graphState?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CourtCasePage {
  records: CourtCase[];
  total: number;
  current: number;
  size: number;
}

export interface CourtCaseCreateRequest {
  title: string;
  userSide: 'PLAINTIFF' | 'DEFENDANT';
  caseSummary?: string;
  userObjective?: string;
  documentIds: number[];
}

export interface CourtGraphSnapshot {
  tenantId: number;
  caseId: number;
  graphState: 'READY' | 'PROJECTING' | 'UNAVAILABLE';
  pendingEventCount: number;
  nodes: Array<{ businessId: string; type: string; label: string; status?: string; properties?: Record<string, unknown> }>;
  edges: Array<{ businessId: string; sourceBusinessId: string; targetBusinessId: string; type: string; label: string; properties?: Record<string, unknown> }>;
  nodeCount: number;
  edgeCount: number;
  truncated: boolean;
  warning?: string;
}

export interface CourtSuggestion {
  suggestionId: number;
  suggestionType: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'RESOLVED' | 'IGNORED';
  missingDescription: string;
  recommendedMaterialsJson: string;
  rationale: string;
  potentialImpact: string;
}

export interface CourtJudgeOutput {
  focusIssues: string[];
  acceptedFacts: string[];
  rejectedFacts: string[];
  unfavorableToPartyA: string[];
  unfavorableToPartyB: string[];
  openQuestions: string[];
}

export interface CourtJudgmentReport {
  reportId: number;
  status: string;
  focusIssuesJson: string;
  acceptedFactsJson: string;
  rejectedFactsJson: string;
  unfavorableToPlaintiffJson: string;
  unfavorableToDefendantJson: string;
  judgmentPointsJson: string;
  openQuestionsJson: string;
  watermark: string;
}

export function listCourtCases(pageNo = 1, pageSize = 10) {
  return apiGet<CourtCasePage>(`/api/smart-court/cases?pageNo=${pageNo}&pageSize=${pageSize}`);
}

export function createCourtCase(payload: CourtCaseCreateRequest) {
  return apiPost<CourtCase>('/api/smart-court/cases', payload);
}

export function updateCourtCase(caseId: number, payload: Partial<CourtCaseCreateRequest>) {
  return apiPut<CourtCase>(`/api/smart-court/cases/${caseId}`, payload);
}

export function confirmCourtFacts(caseId: number) {
  return apiPost<CourtCase>(`/api/smart-court/cases/${caseId}/confirm-facts`);
}

export function startCourtHearing(caseId: number) {
  return apiPost<CourtCase>(`/api/smart-court/cases/${caseId}/start-hearing`);
}

export function deleteCourtCase(caseId: number) {
  return apiDelete<void>(`/api/smart-court/cases/${caseId}`);
}

export function getCourtGraph(caseId: number, params: { focusClaimId?: string; hops?: number; limit?: number } = {}) {
  const search = new URLSearchParams();
  if (params.focusClaimId) search.set('focusClaimId', params.focusClaimId);
  if (params.hops) search.set('hops', String(params.hops));
  if (params.limit) search.set('limit', String(params.limit));
  const query = search.toString();
  return apiGet<CourtGraphSnapshot>(`/api/smart-court/cases/${caseId}/graph${query ? `?${query}` : ''}`);
}

export function listCourtSuggestions(caseId: number, status?: string) {
  return apiGet<CourtSuggestion[]>(`/api/smart-court/cases/${caseId}/suggestions${status ? `?status=${status}` : ''}`);
}

export function ignoreCourtSuggestion(caseId: number, suggestionId: number) {
  return apiPost<void>(`/api/smart-court/cases/${caseId}/suggestions/${suggestionId}/ignore`);
}

export function resolveCourtSuggestion(caseId: number, suggestionId: number) {
  return apiPost<void>(`/api/smart-court/cases/${caseId}/suggestions/${suggestionId}/resolve`);
}

export function generateCourtReport(caseId: number, roundId: number | null, judgeOutput: CourtJudgeOutput) {
  return apiPost<CourtJudgmentReport>(`/api/smart-court/cases/${caseId}/judgment-report`, { roundId, judgeOutput });
}

export function auditCourtReportExport(caseId: number, exportFormat: string) {
  return apiPost<void>(`/api/smart-court/cases/${caseId}/judgment-report/export-audit?exportFormat=${encodeURIComponent(exportFormat)}`);
}
