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
            <button type="button" :disabled="!!activeAction" @click="startHearing">{{ activeAction === 'startHearingState' ? '开庭中...' : '开庭' }}</button>
            <button type="button" :disabled="!!activeAction" @click="loadGraphAndSuggestions">刷新图谱/建议</button>
            <button class="danger" type="button" :disabled="!!activeAction" @click="requestDeleteCase">删除</button>
          </div>

          <div class="dialogue-box">
            <p class="system-line">{{ hearingStatusText }}</p>
            <p>核心目标：{{ activeCase.userObjective || '未填写' }}</p>
            <textarea v-model="userStatement" rows="4" placeholder="请输入本轮用户发言，点击“用户发言”时发言不能为空。"></textarea>
            <div class="speech-actions">
              <button type="button" :disabled="speechDisabled" @click="startHearingWithUserStatement">{{ activeAction === 'streamHearing' ? '发言中...' : '用户发言' }}</button>
              <button type="button" :disabled="speechDisabled" @click="startHearingNoStatement">用户本轮暂不补充发言</button>
              <button type="button" :disabled="speechDisabled" @click="startHearingWithAdvisor">辅助律师代理发言</button>
            </div>
            <div v-if="hearingMessages.length" class="hearing-stream">
              <article v-for="item in hearingMessages" :key="item.id" class="hearing-message" :class="item.speaker.toLowerCase()">
                <strong>{{ speakerText(item.speaker) }}</strong>
                <p>{{ item.content }}</p>
                <small v-if="item.modelName || item.tokenUsage">模型：{{ item.modelName || '-' }} · token {{ item.tokenUsage ?? 0 }}</small>
              </article>
            </div>
            <div v-if="operationLogs.length" class="operation-log">
              <h4>操作反馈</h4>
              <p v-for="item in operationLogs" :key="item.id">{{ item.text }}</p>
            </div>
          </div>

          <div v-if="hearingRecords.length" class="records-box">
            <h4>庭审记录</h4>
            <article v-for="record in hearingRecords" :key="record.roundId" class="record-card">
              <strong>第 {{ record.roundNo }} 轮 · {{ record.state }}</strong>
              <p>{{ record.startedAt || '-' }} 至 {{ record.endedAt || '-' }}</p>
              <div class="record-messages">
                <p v-for="message in record.messages" :key="message.argumentId || message.messageId">
                  <b>{{ speakerText(message.speaker) }}：</b>{{ message.content }}
                </p>
              </div>
            </article>
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
              <button type="button" :disabled="!!activeAction" @click="generateReport">根据最近法官发言生成报告</button>
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
        <div class="graph-legend">
          <span v-for="item in graphLegendItems" :key="item.key"><i :style="{ background: item.color }"></i>{{ item.label }}</span>
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

    <Teleport to="body">
      <div v-if="deleteDialogVisible" class="detail-panel" role="dialog" aria-modal="true">
        <div class="detail-modal confirm-dialog">
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
    </Teleport>

    <Teleport to="body">
      <div v-if="graphDetail" class="detail-panel" role="dialog" aria-modal="true">
        <div class="detail-modal graph-dialog">
          <div class="graph-detail-head">
            <div>
              <p class="eyebrow">{{ graphDetail.kind === 'node' ? '图谱节点' : '图谱关系' }}</p>
              <h3>{{ graphDetail.title }}</h3>
            </div>
            <button type="button" @click="graphDetail = null">关闭</button>
          </div>
          <p>{{ graphDetail.summary }}</p>
          <dl>
            <template v-for="item in graphDetail.properties" :key="item.key">
              <dt>{{ item.key }}</dt>
              <dd>{{ item.value }}</dd>
            </template>
          </dl>
        </div>
      </div>
    </Teleport>
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
  listCourtHearingRecords,
  listCourtCases,
  listCourtSuggestions,
  resolveCourtSuggestion,
  startCourtHearing,
  streamCourtHearing,
  type CourtCase,
  type CourtGraphSnapshot,
  type CourtHearingRecord,
  type CourtHearingStreamEvent,
  type CourtJudgmentReport,
  type CourtSuggestion,
} from '../api/court';

const loading = ref(false);
const cases = ref<CourtCase[]>([]);
const activeCase = ref<CourtCase | null>(null);
const graph = ref<CourtGraphSnapshot | null>(null);
const suggestions = ref<CourtSuggestion[]>([]);
const hearingRecords = ref<CourtHearingRecord[]>([]);
const report = ref<CourtJudgmentReport | null>(null);
const graphRef = ref<HTMLElement | null>(null);
let chart: echarts.ECharts | null = null;
let localIdSeed = 0;
const form = ref({ title: '', userSide: 'PLAINTIFF' as 'PLAINTIFF' | 'DEFENDANT', caseSummary: '', userObjective: '' });
const documentIdsText = ref('');
const activeAction = ref('');
const deleteDialogVisible = ref(false);
const notice = ref<{ type: 'success' | 'error'; text: string } | null>(null);
const operationLogs = ref<Array<{ id: number; text: string }>>([]);
const hearingMessages = ref<Array<{ id: number; speaker: string; content: string; modelName?: string; tokenUsage?: number }>>([]);
const hearingStatusText = ref('请先点击开庭，开庭后再选择用户方发言方式。');
const userStatement = ref('');
const graphDetail = ref<{ kind: 'node' | 'edge'; title: string; summary: string; properties: Array<{ key: string; value: string }> } | null>(null);
let stopHearingStream: (() => void) | null = null;

const highSuggestions = computed(() => suggestions.value.filter((item) => item.severity === 'HIGH' && item.status === 'OPEN'));
const speechDisabled = computed(() => !!activeAction.value || activeCase.value?.status !== 'HEARING');
const graphLegendItems = [
  { key: 'USER', label: '用户发言', color: '#2563eb' },
  { key: 'USER_ADVISOR', label: '辅助律师', color: '#2f6b4f' },
  { key: 'OPPONENT', label: '对方代理人', color: '#b42318' },
  { key: 'JUDGE', label: '法官', color: '#b7791f' },
  { key: 'Evidence', label: '证据', color: '#64748b' },
  { key: 'Case', label: '案件', color: '#1f2937' },
];

onMounted(() => {
  loadCases();
  window.addEventListener('resize', resizeGraph);
});
onBeforeUnmount(() => {
  stopHearingStream?.();
  window.removeEventListener('resize', resizeGraph);
  chart?.dispose();
});

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
  hearingMessages.value = [];
  await loadGraphAndSuggestions();
  await loadHearingRecords();
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
  activeAction.value = 'startHearingState';
  try {
    activeCase.value = await startCourtHearing(activeCase.value.caseId);
    await loadCases();
    await loadGraphAndSuggestions();
    hearingStatusText.value = activeCase.value.userSide === 'PLAINTIFF'
      ? '已开庭。用户作为原告，请先选择用户发言或辅助律师代理发言。'
      : '已开庭。用户作为被告，本轮将先由对方代理人发言，再进入用户方发言。';
    pushLog(`开庭成功，当前状态：${statusText(activeCase.value.status)}`);
    showNotice('success', '已进入开庭状态');
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    activeAction.value = '';
  }
}

async function startHearingWithUserStatement() {
  const statement = userStatement.value.trim();
  if (!statement) {
    showNotice('error', '用户发言不能为空');
    return;
  }
  await startHearingWithPayload({ userStatement: statement, advisorAutoSpeak: false, userNoStatement: false });
}

async function startHearingNoStatement() {
  await startHearingWithPayload({ userNoStatement: true, advisorAutoSpeak: false });
}

async function startHearingWithAdvisor() {
  await startHearingWithPayload({ advisorAutoSpeak: true });
}

async function startHearingWithPayload(payload: { userStatement?: string; userNoStatement?: boolean; advisorAutoSpeak?: boolean }) {
  if (!activeCase.value) return;
  activeAction.value = 'streamHearing';
  hearingMessages.value = [];
  hearingStatusText.value = '庭审流式生成中...';
  try {
    await runHearingStream(activeCase.value.caseId, payload);
    await loadCases();
    await loadGraphAndSuggestions();
    await loadHearingRecords();
    pushLog('本轮流式庭审已完成');
    showNotice('success', '本轮庭审生成完成');
  } catch (err) {
    showNotice('error', errorMessage(err));
  } finally {
    activeAction.value = '';
  }
}

function runHearingStream(caseId: number, payload: { userStatement?: string; userNoStatement?: boolean; advisorAutoSpeak?: boolean }) {
  return new Promise<void>((resolve, reject) => {
    stopHearingStream?.();
    stopHearingStream = streamCourtHearing(caseId, payload, (eventName, event) => {
      handleHearingEvent(eventName, event, resolve, reject);
    });
  });
}

function handleHearingEvent(eventName: string, event: CourtHearingStreamEvent, resolve: () => void, reject: (err: Error) => void) {
  if (eventName === 'round-start') {
    hearingStatusText.value = '庭审开始，正在生成多角色发言...';
    pushLog('庭审流式连接已建立');
    return;
  }
  if (eventName === 'role-start') {
    hearingStatusText.value = `${speakerText(event.speaker || 'SYSTEM')}发言中...`;
    pushLog(event.content || `${speakerText(event.speaker || 'SYSTEM')}开始发言`);
    return;
  }
  if (eventName === 'role-delta') {
    hearingStatusText.value = `${speakerText(event.speaker || 'SYSTEM')}发言生成中...`;
    return;
  }
  if (eventName === 'role-complete') {
    if (event.content) {
      const message = {
        id: nextLocalId(),
        speaker: event.speaker || 'SYSTEM',
        content: event.content,
        modelName: event.modelName,
        tokenUsage: event.tokenUsage,
      };
      hearingMessages.value.push(message);
    }
    if (eventName === 'role-complete') {
      pushLog(`${speakerText(event.speaker || 'SYSTEM')}发言完成`);
    }
    return;
  }
  if (eventName === 'round-complete') {
    hearingStatusText.value = '本轮庭审已完成';
    stopHearingStream = null;
    resolve();
    return;
  }
  if (eventName === 'error') {
    hearingStatusText.value = event.content || '服务繁忙，请稍后重试';
    stopHearingStream = null;
    reject(new Error(event.content || '服务繁忙，请稍后重试'));
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

async function loadHearingRecords() {
  if (!activeCase.value) return;
  hearingRecords.value = await listCourtHearingRecords(activeCase.value.caseId);
}

function renderGraph() {
  const el = graphRef.value;
  if (!el || !graph.value) return;
  chart ||= echarts.init(el);
  chart.off('click');
  chart.setOption({
    tooltip: {
      formatter(params: any) {
        return graphTooltip(params);
      },
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      force: { repulsion: 160, edgeLength: 90 },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 10],
      data: graph.value.nodes.map((node) => ({
        id: node.businessId,
        name: node.label || node.businessId,
        category: node.type,
        symbolSize: node.type === 'Argument' ? 52 : 46,
        itemStyle: { color: graphNodeColor(node) },
        value: node,
      })),
      links: graph.value.edges.map((edge) => ({
        source: edge.sourceBusinessId,
        target: edge.targetBusinessId,
        name: edge.type,
        value: edge,
        lineStyle: { width: 2, color: graphEdgeColor(edge.type) },
        label: { show: true, formatter: relationText(edge.type) },
      })),
      label: { show: true, fontSize: 10 },
      edgeLabel: { show: true, fontSize: 9, color: '#405145' },
      categories: [...new Set(graph.value.nodes.map((node) => node.type))].map((name) => ({ name })),
    }],
  });
  chart.on('click', (params: any) => {
    if (params.dataType === 'node') {
      openGraphNodeDetail(params.data?.value);
    } else if (params.dataType === 'edge') {
      openGraphEdgeDetail(params.data?.value);
    }
  });
}

function resizeGraph() {
  chart?.resize();
}

function graphTooltip(params: any) {
  const value = params.data?.value;
  if (!value) return '';
  if (params.dataType === 'edge') {
    return `<strong>${escapeHtml(value.type || value.label || '关系')}</strong><br/>${escapeHtml(value.sourceBusinessId || '')} → ${escapeHtml(value.targetBusinessId || '')}`;
  }
  const summary = value.properties?.content || value.properties?.title || value.label || value.businessId;
  return `<strong>${escapeHtml(value.label || value.businessId)}</strong><br/>${escapeHtml(value.type || '节点')} · ${escapeHtml(shortText(String(summary || ''), 80))}`;
}

function openGraphNodeDetail(node: any) {
  if (!node) return;
  const summary = node.properties?.content || node.properties?.title || node.label || node.businessId;
  graphDetail.value = {
    kind: 'node',
    title: node.label || node.businessId,
    summary: shortText(String(summary || ''), 260),
    properties: propertyEntries(node.properties || {}),
  };
}

function openGraphEdgeDetail(edge: any) {
  if (!edge) return;
  graphDetail.value = {
    kind: 'edge',
    title: edge.type || edge.label || '关系',
    summary: `${edge.sourceBusinessId || '-'} → ${edge.targetBusinessId || '-'}`,
    properties: propertyEntries(edge.properties || {}),
  };
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
    report.value = await generateCourtReport(activeCase.value.caseId);
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

function graphNodeColor(node: CourtGraphSnapshot['nodes'][number]) {
  const speakerRole = String(node.properties?.speakerRole || '');
  const map: Record<string, string> = {
    USER: '#2563eb',
    USER_ADVISOR: '#2f6b4f',
    OPPONENT: '#b42318',
    JUDGE: '#b7791f',
  };
  if (speakerRole && map[speakerRole]) return map[speakerRole];
  if (node.type === 'Evidence') return '#64748b';
  if (node.type === 'Case') return '#1f2937';
  return '#7c8b7f';
}

function graphEdgeColor(type: string) {
  const map: Record<string, string> = {
    PRESENTS_ARGUMENT: '#2563eb',
    ADVISES_ARGUMENT: '#2f6b4f',
    OPPOSES_ARGUMENT: '#b42318',
    REBUTS_ARGUMENT: '#dc2626',
    CHALLENGES_ARGUMENT: '#ea580c',
    SUPPORTS_ARGUMENT: '#16a34a',
    SUMMARIZES_ARGUMENT: '#b7791f',
    HAS_EVIDENCE: '#64748b',
  };
  return map[type] || '#7c8b7f';
}

function relationText(type: string) {
  const map: Record<string, string> = {
    PRESENTS_ARGUMENT: '提出观点',
    ADVISES_ARGUMENT: '代理发言',
    OPPOSES_ARGUMENT: '对方观点',
    REBUTS_ARGUMENT: '反驳',
    CHALLENGES_ARGUMENT: '质疑',
    SUPPORTS_ARGUMENT: '支持',
    SUMMARIZES_ARGUMENT: '归纳',
    HAS_EVIDENCE: '包含证据',
  };
  return map[type] || type;
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

function speakerText(speaker: string) {
  const map: Record<string, string> = { OPPONENT: '对方代理人', USER_ADVISOR: '用户辅助律师', JUDGE: '法官', SYSTEM: '系统', USER: '用户' };
  return map[speaker] || speaker;
}

function propertyEntries(properties: Record<string, unknown>) {
  return Object.entries(properties).slice(0, 20).map(([key, value]) => ({
    key,
    value: typeof value === 'object' ? JSON.stringify(value) : String(value ?? ''),
  }));
}

function shortText(text: string, max: number) {
  return text.length <= max ? text : `${text.slice(0, max)}...`;
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function pushLog(text: string) {
  operationLogs.value.unshift({ id: nextLocalId(), text: `${new Date().toLocaleTimeString()} ${text}` });
  operationLogs.value = operationLogs.value.slice(0, 6);
}

function nextLocalId() {
  localIdSeed += 1;
  return Date.now() * 1000 + localIdSeed;
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
.court-page { display: grid; gap: 1rem; min-height: 100vh; }
.hero-panel, .panel { border: 1px solid rgba(29,43,35,.12); border-radius: 24px; background: rgba(255,255,255,.42); box-shadow: 0 18px 42px rgba(29,43,35,.08); }
.hero-panel { display: flex; justify-content: space-between; gap: 1rem; padding: 1.2rem; align-items: center; background: linear-gradient(135deg, rgba(50,92,73,.16), rgba(192,147,71,.14)); }
.hero-panel h2, .panel h3 { margin: .2rem 0; }
.hero-panel p, .graph-head p, .case-head p { color: var(--ink-soft); margin: .25rem 0 0; }
.notice { border-radius: 18px; padding: .85rem 1rem; font-weight: 700; box-shadow: 0 12px 28px rgba(29,43,35,.08); }
.notice.success { border: 1px solid rgba(50,92,73,.24); background: rgba(50,92,73,.12); color: var(--forest-deep); }
.notice.error { border: 1px solid rgba(127,29,29,.28); background: rgba(127,29,29,.08); color: #7f1d1d; }
.court-grid { display: grid; grid-template-columns: 320px minmax(310px, 1fr) 610px; gap: 1rem; align-items: stretch; min-height: calc(100vh - 150px); }
.panel { padding: 1rem; min-height: 0; }
.cases-panel, .graph-panel { height: calc(100vh - 150px); overflow: auto; }
.hearing-panel { height: calc(100vh - 150px); overflow: auto; }
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
.dialogue-box { max-height: 430px; overflow: auto; }
.system-line { color: var(--forest-deep); font-weight: 700; }
.speech-actions { display: flex; flex-wrap: wrap; gap: .6rem; margin-top: .7rem; }
.speech-actions button { background: rgba(29,43,35,.88); }
.hearing-stream { display: grid; gap: .7rem; margin-top: .8rem; }
.hearing-message { border: 1px solid rgba(29,43,35,.1); border-radius: 16px; padding: .75rem; background: rgba(255,255,255,.56); }
.hearing-message strong { color: var(--forest-deep); }
.hearing-message p { white-space: pre-wrap; margin: .45rem 0; color: var(--ink); line-height: 1.65; }
.hearing-message small { color: var(--ink-soft); }
.hearing-message.opponent { border-color: rgba(127,29,29,.18); }
.hearing-message.user_advisor { border-color: rgba(50,92,73,.22); }
.hearing-message.judge { border-color: rgba(192,147,71,.32); }
.records-box { margin-top: 1rem; display: grid; gap: .7rem; }
.record-card { border: 1px solid rgba(29,43,35,.1); border-radius: 18px; padding: .85rem; background: rgba(255,255,255,.4); }
.record-card > p, .record-messages p { margin: .35rem 0; color: var(--ink-soft); line-height: 1.55; }
.record-messages { max-height: 220px; overflow: auto; }
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
.graph-canvas { height: 490px; border-radius: 18px; background: radial-gradient(circle at 50% 20%, rgba(50,92,73,.12), rgba(255,255,255,.35)); }
.graph-legend { display: flex; flex-wrap: wrap; gap: .55rem; margin: .75rem 0; color: var(--ink-soft); font-size: .82rem; }
.graph-legend span { display: inline-flex; align-items: center; gap: .35rem; border: 1px solid rgba(29,43,35,.1); border-radius: 999px; padding: .28rem .55rem; background: rgba(255,255,255,.52); }
.graph-legend i { width: .7rem; height: .7rem; border-radius: 999px; display: inline-block; }
.empty-state { display: grid; place-items: center; min-height: 420px; color: var(--ink-soft); }
.detail-panel { position: fixed; inset: 0; z-index: 9999; display: grid; place-items: center; padding: 1rem; background: rgba(19, 28, 23, 0.46); backdrop-filter: blur(6px); }
.detail-modal { width: min(560px, 100%); padding: 1rem; border: 1px solid rgba(29,43,35,.12); border-radius: 24px; background: var(--paper-strong); box-shadow: 0 28px 70px rgba(16,24,20,.32); }
.confirm-dialog { width: min(440px, 100%); border-color: rgba(127,29,29,.22); }
.confirm-dialog h3 { margin: .2rem 0 .5rem; }
.confirm-dialog p { color: var(--ink-soft); line-height: 1.65; }
.graph-dialog { width: min(620px, 100%); max-height: 82vh; overflow: auto; }
.graph-detail-head { display: flex; justify-content: space-between; gap: 1rem; align-items: flex-start; }
.graph-detail-head h3 { margin: .2rem 0 .5rem; }
.graph-dialog p { color: var(--ink-soft); line-height: 1.65; }
.graph-dialog dl { display: grid; grid-template-columns: 150px 1fr; gap: .45rem .8rem; margin-top: 1rem; }
.graph-dialog dt { color: var(--forest-deep); font-weight: 700; }
.graph-dialog dd { margin: 0; color: var(--ink-soft); word-break: break-all; }
.dialog-actions { display: flex; justify-content: flex-end; gap: .7rem; margin-top: 1rem; }
@media (max-width: 1180px) { .court-grid { grid-template-columns: 1fr; min-height: auto; } .cases-panel, .graph-panel, .hearing-panel { height: auto; max-height: none; } .panel { min-height: auto; } }
</style>
