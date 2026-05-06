<template>
  <section class="court-page">
    <header class="hero-panel">
      <div>
        <p class="eyebrow">SMART COURT</p>
        <h2>合同纠纷智能小法庭</h2>
        <p>围绕证据链、庭审轮次、补证建议与模拟裁判报告完成一案到底的推演。</p>
      </div>
      <button class="primary-btn" type="button" :disabled="loading" @click="loadCases">刷新案件</button>
    </header>

    <div v-if="notice" class="notice" :class="notice.type">{{ notice.text }}</div>

    <div class="court-grid">
      <aside class="panel cases-panel">
        <h3>案件与材料</h3>
        <form class="case-form" @submit.prevent="createCase">
          <input v-model="form.title" placeholder="案件标题" />
          <select v-model="form.userSide">
            <option value="PLAINTIFF">用户作为原告</option>
            <option value="DEFENDANT">用户作为被告</option>
          </select>
          <textarea v-model="form.caseSummary" rows="4" placeholder="案件简述"></textarea>
          <textarea v-model="form.userObjective" rows="3" placeholder="核心诉求或抗辩目标"></textarea>
          <input v-model="documentIdsText" placeholder="证据文档ID，逗号分隔" />
          <button class="primary-btn" type="submit" :disabled="loading">创建案件</button>
        </form>

        <div class="case-list">
          <button
            v-for="item in cases"
            :key="item.caseId"
            class="case-card"
            :class="{ active: activeCase?.caseId === item.caseId }"
            type="button"
            @click="selectCase(item)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ statusText(item.status) }} · {{ item.userSide === 'PLAINTIFF' ? '原告' : '被告' }}</span>
          </button>
        </div>
      </aside>

      <main class="panel hearing-panel">
        <template v-if="activeCase">
          <div class="case-head">
            <div>
              <p class="eyebrow">CASE #{{ activeCase.caseId }}</p>
              <h3>{{ activeCase.title }}</h3>
              <p>{{ activeCase.caseSummary || '暂无案件摘要' }}</p>
            </div>
            <span class="status-pill">{{ statusText(activeCase.status) }}</span>
          </div>

          <div class="action-row">
            <button type="button" :disabled="!!activeAction" @click="confirmFacts">{{ activeAction === 'confirmFacts' ? '确认中...' : '确认要素' }}</button>
            <button type="button" :disabled="!!activeAction" @click="startHearing">{{ activeAction === 'startHearing' ? '开庭中...' : '开庭' }}</button>
            <button type="button" :disabled="!!activeAction" @click="loadGraphAndSuggestions">刷新图谱/建议</button>
            <button class="danger" type="button" :disabled="!!activeAction" @click="requestDeleteCase">删除</button>
          </div>

          <div class="dialogue-box">
            <p class="system-line">当前后端已接入轮次状态机与 SSE 安全工具，前端流式庭审将在接口完善后直接接入。</p>
            <p>核心目标：{{ activeCase.userObjective || '未填写' }}</p>
            <div v-if="operationLogs.length" class="operation-log">
              <h4>操作反馈</h4>
              <p v-for="item in operationLogs" :key="item.id">{{ item.text }}</p>
            </div>
          </div>

          <div class="suggestion-box" v-if="highSuggestions.length">
            <h4>最后陈述前高严重度补证建议</h4>
            <article v-for="item in highSuggestions" :key="item.suggestionId" class="suggestion high">
              <strong>{{ item.missingDescription }}</strong>
              <p>{{ item.potentialImpact || item.rationale }}</p>
            </article>
          </div>

          <section class="report-panel">
            <h4>模拟裁判报告</h4>
            <div class="report-form">
              <textarea v-model="judgeJson" rows="8" placeholder="粘贴 AI 法官 JSON 输出"></textarea>
              <button type="button" @click="generateReport">生成报告</button>
              <button type="button" :disabled="!report" @click="auditExport">PDF 导出审计</button>
            </div>
            <article v-if="report" class="report-card">
              <div class="watermark">{{ report.watermark }}</div>
              <h5>争议焦点</h5>
              <p>{{ parseList(report.focusIssuesJson).join('；') }}</p>
              <h5>模拟裁判观点</h5>
              <p>{{ parseList(report.judgmentPointsJson).join('；') }}</p>
            </article>
          </section>
        </template>
        <div v-else class="empty-state">请选择或创建一个案件。</div>
      </main>

      <aside class="panel graph-panel">
        <div class="graph-head">
          <div>
            <h3>证据链图谱</h3>
            <p>{{ graph?.graphState || '未加载' }} · pending {{ graph?.pendingEventCount ?? 0 }}</p>
          </div>
          <span v-if="graph?.warning" class="warning">{{ graph.warning }}</span>
        </div>
        <div ref="graphRef" class="graph-canvas"></div>
        <div class="suggestions">
          <h4>补证建议</h4>
          <article v-for="item in suggestions" :key="item.suggestionId" class="suggestion" :class="item.severity.toLowerCase()">
            <div>
              <strong>{{ item.suggestionType }}</strong>
              <span>{{ item.severity }} · {{ item.status }}</span>
            </div>
            <p>{{ item.missingDescription }}</p>
            <div class="mini-actions">
              <button type="button" :disabled="!!activeAction" @click="ignoreSuggestion(item.suggestionId)">忽略</button>
              <button type="button" :disabled="!!activeAction" @click="resolveSuggestion(item.suggestionId)">解除</button>
            </div>
          </article>
        </div>
      </aside>
    </div>

    <div v-if="deleteDialogVisible" class="modal-mask" role="dialog" aria-modal="true">
      <div class="confirm-dialog">
        <p class="eyebrow">危险操作</p>
        <h3>确认删除案件？</h3>
        <p>案件「{{ activeCase?.title }}」将被软删除，并触发图谱清理事件。该操作不可在当前页面直接撤销。</p>
        <div class="dialog-actions">
          <button type="button" @click="deleteDialogVisible = false">取消</button>
          <button class="danger" type="button" :disabled="activeAction === 'deleteCase'" @click="deleteCase">
            {{ activeAction === 'deleteCase' ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import {
  auditCourtReportExport,
  confirmCourtFacts,
  createCourtCase as createCourtCaseApi,
  deleteCourtCase as deleteCourtCaseApi,
  generateCourtReport,
  getCourtGraph,
  ignoreCourtSuggestion,
  listCourtCases,
  listCourtSuggestions,
  resolveCourtSuggestion,
  startCourtHearing,
  type CourtCase,
  type CourtGraphSnapshot,
  type CourtJudgmentReport,
  type CourtSuggestion,
} from '../api/court';

const loading = ref(false);
const cases = ref<CourtCase[]>([]);
const activeCase = ref<CourtCase | null>(null);
const graph = ref<CourtGraphSnapshot | null>(null);
const suggestions = ref<CourtSuggestion[]>([]);
const report = ref<CourtJudgmentReport | null>(null);
const graphRef = ref<HTMLElement | null>(null);
let chart: echarts.ECharts | null = null;
const form = ref({ title: '', userSide: 'PLAINTIFF' as 'PLAINTIFF' | 'DEFENDANT', caseSummary: '', userObjective: '' });
const documentIdsText = ref('');
const judgeJson = ref('');
const activeAction = ref('');
const deleteDialogVisible = ref(false);
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
const operationLogs = ref<Array<{ id: number; text: string }>>([]);

const highSuggestions = computed(() => suggestions.value.filter((item) => item.severity === 'HIGH' && item.status === 'OPEN'));

onMounted(loadCases);
onBeforeUnmount(() => chart?.dispose());

async function loadCases() {
  loading.value = true;
  try {
    const page = await listCourtCases();
    cases.value = page.records || [];
    if (!activeCase.value && cases.value.length) {
      await selectCase(cases.value[0]);
    }
  } finally {
    loading.value = false;
  }
}

async function createCase() {
  loading.value = true;
  try {
    const documentIds = documentIdsText.value.split(/[,，\s]+/).map((item) => Number(item)).filter(Boolean);
    const created = await createCourtCaseApi({ ...form.value, documentIds });
    activeCase.value = created;
    await loadCases();
    await loadGraphAndSuggestions();
    pushLog(`案件「${created.title}」创建成功`);
    showNotice('success', '案件创建成功');
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    loading.value = false;
  }
}

async function selectCase(item: CourtCase) {
  activeCase.value = item;
  report.value = null;
  await loadGraphAndSuggestions();
}

async function confirmFacts() {
  if (!activeCase.value) return;
  activeAction.value = 'confirmFacts';
  try {
    activeCase.value = await confirmCourtFacts(activeCase.value.caseId);
    await loadCases();
    await loadGraphAndSuggestions();
    pushLog(`要素确认成功，当前状态：${statusText(activeCase.value.status)}`);
    showNotice('success', '要素已确认');
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    activeAction.value = '';
  }
}

async function startHearing() {
  if (!activeCase.value) return;
  activeAction.value = 'startHearing';
  try {
    activeCase.value = await startCourtHearing(activeCase.value.caseId);
    await loadCases();
    await loadGraphAndSuggestions();
    pushLog(`开庭成功，当前状态：${statusText(activeCase.value.status)}`);
    showNotice('success', '已进入开庭状态');
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    activeAction.value = '';
  }
}

function requestDeleteCase() {
  if (!activeCase.value) return;
  deleteDialogVisible.value = true;
}

async function deleteCase() {
  if (!activeCase.value) return;
  const title = activeCase.value.title;
  activeAction.value = 'deleteCase';
  try {
    await deleteCourtCaseApi(activeCase.value.caseId);
    deleteDialogVisible.value = false;
    activeCase.value = null;
    graph.value = null;
    suggestions.value = [];
    operationLogs.value = [];
    await loadCases();
    showNotice('success', `案件「${title}」已删除`);
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    activeAction.value = '';
  }
}

async function loadGraphAndSuggestions() {
  if (!activeCase.value) return;
  activeAction.value = activeAction.value || 'refreshGraph';
  try {
    graph.value = await getCourtGraph(activeCase.value.caseId, { hops: 2, limit: 200 });
    suggestions.value = await listCourtSuggestions(activeCase.value.caseId);
    await nextTick(renderGraph);
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    if (activeAction.value === 'refreshGraph') {
      activeAction.value = '';
    }
  }
}

function renderGraph() {
  const el = graphRef.value;
  if (!el || !graph.value) return;
  chart ||= echarts.init(el);
  chart.setOption({
    tooltip: {},
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      force: { repulsion: 160, edgeLength: 90 },
      data: graph.value.nodes.map((node) => ({ id: node.businessId, name: node.label || node.businessId, category: node.type, symbolSize: 46 })),
      links: graph.value.edges.map((edge) => ({ source: edge.sourceBusinessId, target: edge.targetBusinessId, name: edge.type })),
      label: { show: true, fontSize: 10 },
      categories: [...new Set(graph.value.nodes.map((node) => node.type))].map((name) => ({ name })),
    }],
  });
}

async function ignoreSuggestion(suggestionId: number) {
  if (!activeCase.value) return;
  activeAction.value = `ignore-${suggestionId}`;
  try {
    await ignoreCourtSuggestion(activeCase.value.caseId, suggestionId);
    await loadGraphAndSuggestions();
    pushLog(`补证建议 #${suggestionId} 已忽略`);
    showNotice('success', '补证建议已忽略');
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    activeAction.value = '';
  }
}

async function resolveSuggestion(suggestionId: number) {
  if (!activeCase.value) return;
  activeAction.value = `resolve-${suggestionId}`;
  try {
    await resolveCourtSuggestion(activeCase.value.caseId, suggestionId);
    await loadGraphAndSuggestions();
    pushLog(`补证建议 #${suggestionId} 已解除`);
    showNotice('success', '补证建议已解除');
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    activeAction.value = '';
  }
}

async function generateReport() {
  if (!activeCase.value) return;
  try {
    const output = JSON.parse(judgeJson.value);
    report.value = await generateCourtReport(activeCase.value.caseId, null, output);
    pushLog('模拟裁判报告生成成功');
    showNotice('success', '报告已生成');
  } catch (err) {
    showNotice('error', errorMessage(err));
  }
}

async function auditExport() {
  if (!activeCase.value) return;
  try {
    await auditCourtReportExport(activeCase.value.caseId, 'PDF');
    showNotice('success', 'PDF 导出审计已记录');
  } catch (err) {
    showNotice('error', errorMessage(err));
  }
}

function parseList(json?: string) {
  try {
    const value = JSON.parse(json || '[]');
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
}

function statusText(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', READY: '可开庭', HEARING: '庭审中', JUDGED: '已裁判', ARCHIVED: '已归档' };
  return map[status] || status;
}

function pushLog(text: string) {
  operationLogs.value.unshift({ id: Date.now(), text: `${new Date().toLocaleTimeString()} ${text}` });
  operationLogs.value = operationLogs.value.slice(0, 6);
}

function showNotice(type: 'success' | 'error', text: string) {
  notice.value = { type, text };
  window.setTimeout(() => {
    if (notice.value?.text === text) {
      notice.value = null;
    }
  }, 3200);
}

function errorMessage(err: unknown) {
  return err instanceof Error ? err.message : String(err || '操作失败');
}
</script>

<style scoped>
.court-page { display: grid; gap: 1rem; }
.hero-panel, .panel { border: 1px solid rgba(29,43,35,.12); border-radius: 24px; background: rgba(255,255,255,.42); box-shadow: 0 18px 42px rgba(29,43,35,.08); }
.hero-panel { display: flex; justify-content: space-between; gap: 1rem; padding: 1.2rem; align-items: center; background: linear-gradient(135deg, rgba(50,92,73,.16), rgba(192,147,71,.14)); }
.hero-panel h2, .panel h3 { margin: .2rem 0; }
.hero-panel p, .graph-head p, .case-head p { color: var(--ink-soft); margin: .25rem 0 0; }
.notice { border-radius: 18px; padding: .85rem 1rem; font-weight: 700; box-shadow: 0 12px 28px rgba(29,43,35,.08); }
.notice.success { border: 1px solid rgba(50,92,73,.24); background: rgba(50,92,73,.12); color: var(--forest-deep); }
.notice.error { border: 1px solid rgba(127,29,29,.28); background: rgba(127,29,29,.08); color: #7f1d1d; }
.court-grid { display: grid; grid-template-columns: 280px minmax(360px, 1fr) 360px; gap: 1rem; align-items: start; }
.panel { padding: 1rem; min-height: 520px; }
.case-form { display: grid; gap: .65rem; }
input, select, textarea { width: 100%; box-sizing: border-box; border: 1px solid rgba(29,43,35,.16); border-radius: 14px; padding: .68rem .78rem; background: rgba(255,255,255,.72); color: var(--ink); }
button, .primary-btn { border: 1px solid rgba(29,43,35,.16); border-radius: 999px; padding: .65rem .95rem; background: var(--ink); color: var(--paper); cursor: pointer; }
button:disabled { opacity: .55; cursor: not-allowed; }
.danger { background: #7f1d1d; }
.case-list { display: grid; gap: .6rem; margin-top: 1rem; }
.case-card { display: grid; gap: .25rem; text-align: left; border-radius: 18px; background: rgba(255,255,255,.6); color: var(--ink); }
.case-card.active { background: linear-gradient(135deg, var(--forest), var(--brass)); color: var(--paper); }
.case-card span { font-size: .82rem; opacity: .78; }
.case-head, .graph-head { display: flex; justify-content: space-between; gap: 1rem; align-items: flex-start; }
.status-pill, .warning { border-radius: 999px; padding: .35rem .65rem; background: rgba(192,147,71,.18); color: var(--forest-deep); font-size: .82rem; }
.action-row { display: flex; flex-wrap: wrap; gap: .6rem; margin: 1rem 0; }
.dialogue-box, .report-card, .suggestion { border: 1px solid rgba(29,43,35,.1); border-radius: 18px; padding: .85rem; background: rgba(255,255,255,.48); }
.system-line { color: var(--forest-deep); font-weight: 700; }
.operation-log { margin-top: .8rem; border-top: 1px solid rgba(29,43,35,.1); padding-top: .7rem; }
.operation-log h4 { margin: 0 0 .45rem; }
.operation-log p { margin: .25rem 0; color: var(--ink-soft); }
.suggestion-box, .report-panel, .suggestions { margin-top: 1rem; }
.suggestion { margin-top: .6rem; }
.suggestion.high { border-color: rgba(127,29,29,.24); background: rgba(127,29,29,.06); }
.suggestion.medium { border-color: rgba(192,147,71,.28); }
.suggestion.low { opacity: .82; }
.suggestion div { display: flex; justify-content: space-between; gap: .5rem; }
.suggestion p { margin: .45rem 0 0; color: var(--ink-soft); }
.mini-actions { justify-content: flex-start !important; margin-top: .6rem; }
.mini-actions button { padding: .38rem .62rem; font-size: .8rem; background: rgba(29,43,35,.8); }
.report-form { display: grid; gap: .7rem; }
.watermark { display: inline-block; border: 1px dashed rgba(127,29,29,.45); color: #7f1d1d; padding: .35rem .6rem; border-radius: 999px; transform: rotate(-2deg); }
.graph-canvas { height: 330px; border-radius: 18px; background: radial-gradient(circle at 50% 20%, rgba(50,92,73,.12), rgba(255,255,255,.35)); }
.empty-state { display: grid; place-items: center; min-height: 420px; color: var(--ink-soft); }
.modal-mask { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 1rem; background: rgba(16,24,20,.48); backdrop-filter: blur(8px); }
.confirm-dialog { width: min(440px, 100%); border: 1px solid rgba(127,29,29,.22); border-radius: 24px; background: var(--paper-strong); padding: 1.25rem; box-shadow: 0 28px 70px rgba(16,24,20,.32); }
.confirm-dialog h3 { margin: .2rem 0 .5rem; }
.confirm-dialog p { color: var(--ink-soft); line-height: 1.65; }
.dialog-actions { display: flex; justify-content: flex-end; gap: .7rem; margin-top: 1rem; }
@media (max-width: 1180px) { .court-grid { grid-template-columns: 1fr; } .panel { min-height: auto; } }
</style>
