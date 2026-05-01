import { onMounted, ref } from 'vue';
import { apiDelete, apiGet, apiPost, apiPostForm, randomRequestId } from '../api/client';
const rules = ref([]);
const fieldDefinitions = ref([]);
const selectedFile = ref(null);
const notice = ref('');
const manualRuleName = ref('');
const manualRuleCode = ref('');
const manualSource = ref('');
const manualSeverity = ref('MEDIUM');
const manualHitThreshold = ref(0.78);
const manualRuleContent = ref('');
const importTitle = ref('');
const importRuleName = ref('');
const importRuleCode = ref('');
const importSource = ref('');
const importSeverity = ref('MEDIUM');
const importHitThreshold = ref(0.78);
const editingFieldId = ref(null);
const fieldCode = ref('');
const fieldName = ref('');
const extractorKind = ref('PARTY_PATTERN');
const fieldPatternExpr = ref('');
const fieldKeywordConfig = ref('');
const fieldRepeatable = ref(false);
const fieldDeduplicate = ref(true);
const fieldEnabled = ref(true);
const fieldSortOrder = ref(10);
const fieldDescription = ref('');
onMounted(refreshAll);
async function refreshAll() {
    rules.value = await apiGet('/api/risk-rules/entries');
    fieldDefinitions.value = await apiGet('/api/risk-rules/fields');
}
function onSelectFile(event) {
    const input = event.target;
    selectedFile.value = input.files?.[0] || null;
}
async function createManualRule() {
    if (!manualRuleName.value.trim() || !manualRuleContent.value.trim()) {
        notice.value = '请填写规则名称和规则内容';
        return;
    }
    await apiPost('/api/risk-rules/manual', {
        requestId: randomRequestId('risk-rule-manual'),
        ruleCode: manualRuleCode.value.trim() || undefined,
        ruleName: manualRuleName.value.trim(),
        source: manualSource.value.trim() || undefined,
        severity: manualSeverity.value,
        hitThreshold: Number(manualHitThreshold.value),
        ruleContent: manualRuleContent.value.trim(),
    });
    manualRuleName.value = '';
    manualRuleCode.value = '';
    manualSource.value = '';
    manualSeverity.value = 'MEDIUM';
    manualHitThreshold.value = 0.78;
    manualRuleContent.value = '';
    notice.value = '手工风险规则已写入 MySQL，并已投递 ES 索引任务';
    await refreshAll();
}
async function importRuleDocument() {
    if (!selectedFile.value) {
        notice.value = '请先选择文件';
        return;
    }
    const formData = new FormData();
    formData.append('requestId', randomRequestId('risk-rule-import'));
    formData.append('file', selectedFile.value);
    if (importTitle.value.trim())
        formData.append('title', importTitle.value.trim());
    if (importRuleName.value.trim())
        formData.append('ruleName', importRuleName.value.trim());
    if (importRuleCode.value.trim())
        formData.append('ruleCode', importRuleCode.value.trim());
    if (importSource.value.trim())
        formData.append('source', importSource.value.trim());
    formData.append('severity', importSeverity.value);
    formData.append('hitThreshold', String(importHitThreshold.value));
    await apiPostForm('/api/risk-rules/import', formData);
    importTitle.value = '';
    importRuleName.value = '';
    importRuleCode.value = '';
    importSource.value = '';
    importSeverity.value = 'MEDIUM';
    importHitThreshold.value = 0.78;
    selectedFile.value = null;
    notice.value = '风险规则文件已导入，规则元数据与 ES 索引已同步创建';
    await refreshAll();
}
async function toggleRule(item) {
    await apiPost(`/api/risk-rules/${item.ruleId}/status`, {
        requestId: randomRequestId('risk-rule-status'),
        enabled: !item.enabled,
    });
    notice.value = `风险规则 ${item.ruleCode} 已${item.enabled ? '停用' : '启用'}`;
    await refreshAll();
}
async function reindexRule(item) {
    await apiPost(`/api/risk-rules/${item.ruleId}/reindex?requestId=${encodeURIComponent(randomRequestId('risk-rule-reindex'))}`);
    notice.value = `风险规则 ${item.ruleCode} 已提交重建索引`;
    await refreshAll();
}
async function deleteRule(item) {
    await apiDelete(`/api/risk-rules/${item.ruleId}?requestId=${encodeURIComponent(randomRequestId('risk-rule-delete'))}`);
    notice.value = `风险规则 ${item.ruleCode} 已删除`;
    await refreshAll();
}
function resetFieldForm() {
    editingFieldId.value = null;
    fieldCode.value = '';
    fieldName.value = '';
    extractorKind.value = 'PARTY_PATTERN';
    fieldPatternExpr.value = '';
    fieldKeywordConfig.value = '';
    fieldRepeatable.value = false;
    fieldDeduplicate.value = true;
    fieldEnabled.value = true;
    fieldSortOrder.value = 10;
    fieldDescription.value = '';
}
function beginEditField(item) {
    editingFieldId.value = item.fieldDefinitionId;
    fieldCode.value = item.fieldCode;
    fieldName.value = item.fieldName;
    extractorKind.value = item.extractorKind;
    fieldPatternExpr.value = item.patternExpr || '';
    fieldKeywordConfig.value = item.keywordConfig || '';
    fieldRepeatable.value = item.repeatable;
    fieldDeduplicate.value = item.deduplicateByNormalized;
    fieldEnabled.value = item.enabled;
    fieldSortOrder.value = item.sortOrder;
    fieldDescription.value = item.description || '';
}
async function saveFieldDefinition() {
    if (!fieldCode.value.trim() || !fieldName.value.trim()) {
        notice.value = '请填写字段编码和字段名称';
        return;
    }
    const payload = {
        requestId: randomRequestId('field-definition-save'),
        fieldCode: fieldCode.value.trim(),
        fieldName: fieldName.value.trim(),
        extractorKind: extractorKind.value,
        patternExpr: fieldPatternExpr.value.trim() || undefined,
        keywordConfig: fieldKeywordConfig.value.trim() || undefined,
        repeatable: fieldRepeatable.value,
        deduplicateByNormalized: fieldDeduplicate.value,
        enabled: fieldEnabled.value,
        sortOrder: Number(fieldSortOrder.value),
        description: fieldDescription.value.trim() || undefined,
    };
    if (editingFieldId.value) {
        await apiPost(`/api/risk-rules/fields/${editingFieldId.value}`, payload);
        notice.value = `字段定义 ${fieldCode.value} 已更新`;
    }
    else {
        await apiPost('/api/risk-rules/fields', payload);
        notice.value = `字段定义 ${fieldCode.value} 已创建`;
    }
    resetFieldForm();
    await refreshAll();
}
async function toggleField(item) {
    await apiPost(`/api/risk-rules/fields/${item.fieldDefinitionId}/status`, {
        requestId: randomRequestId('field-definition-status'),
        enabled: !item.enabled,
    });
    notice.value = `字段定义 ${item.fieldCode} 已${item.enabled ? '停用' : '启用'}`;
    await refreshAll();
}
async function deleteField(item) {
    await apiDelete(`/api/risk-rules/fields/${item.fieldDefinitionId}?requestId=${encodeURIComponent(randomRequestId('field-definition-delete'))}`);
    notice.value = `字段定义 ${item.fieldCode} 已删除`;
    await refreshAll();
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['item-top']} */ ;
/** @type {__VLS_StyleScopedClasses['top-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['field-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "rule-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "top-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "section-block" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "grid form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "例如：报销缺少发票风险",
});
(__VLS_ctx.manualRuleName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "可选，不填则自动生成",
});
(__VLS_ctx.manualRuleCode);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "例如：制度专员手工录入",
});
(__VLS_ctx.manualSource);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.manualSeverity),
    ...{ class: "console-select" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "HIGH",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "MEDIUM",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "LOW",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    min: "0",
    max: "1",
    step: "0.01",
});
(__VLS_ctx.manualHitThreshold);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.manualRuleContent),
    rows: "6",
    placeholder: "请输入风险规则原文，保存后会同时写入 MySQL 与 ES。",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.createManualRule) },
    ...{ class: "primary-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "section-block" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "grid form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "可选，不填则使用文件名",
});
(__VLS_ctx.importTitle);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "可选，不填则使用文档标题",
});
(__VLS_ctx.importRuleName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "可选，不填则自动生成",
});
(__VLS_ctx.importRuleCode);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "例如：制度库 / 扫描件上传",
});
(__VLS_ctx.importSource);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.importSeverity),
    ...{ class: "console-select" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "HIGH",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "MEDIUM",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "LOW",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    min: "0",
    max: "1",
    step: "0.01",
});
(__VLS_ctx.importHitThreshold);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onSelectFile) },
    type: "file",
    accept: ".pdf,.doc,.docx,.txt,.md",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.importRuleDocument) },
    ...{ class: "primary-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refreshAll) },
    ...{ class: "ghost-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-list" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.rules))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (item.ruleId),
        ...{ class: "item-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-top" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (item.ruleName);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
    (item.ruleCode);
    (item.ruleSourceType);
    (item.severity);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status-pill" },
        'data-status': (item.enabled ? 'ENABLED' : 'DISABLED'),
    });
    (item.enabled ? 'ENABLED' : 'DISABLED');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
    (item.documentTitle || '-');
    (item.documentStatus || '-');
    (item.documentIndexStatus || '-');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
    (item.documentSource || '-');
    (item.hitThreshold ?? '-');
    if (item.ruleContent) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "evidence" },
        });
        (item.ruleContent);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.toggleRule(item);
            } },
        ...{ class: "ghost-btn" },
    });
    (item.enabled ? '停用' : '启用');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.reindexRule(item);
            } },
        ...{ class: "ghost-btn" },
        disabled: (!item.documentId),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.deleteRule(item);
            } },
        ...{ class: "warn-btn" },
    });
}
if (!__VLS_ctx.rules.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
}
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
    });
    (__VLS_ctx.notice);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.resetFieldForm) },
    ...{ class: "ghost-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "grid field-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "例如：party_a",
});
(__VLS_ctx.fieldCode);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "例如：甲方",
});
(__VLS_ctx.fieldName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.extractorKind),
    ...{ class: "console-select" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "PARTY_PATTERN",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "AMOUNT_PATTERN",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "DATE_KEYWORD",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "KEYWORD_LINE",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    min: "0",
    step: "1",
});
(__VLS_ctx.fieldSortOrder);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "field-switches selection-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "selection-chip selection-chip--soft" },
    ...{ class: ({ 'selection-chip--active': __VLS_ctx.fieldRepeatable }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "checkbox",
});
(__VLS_ctx.fieldRepeatable);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "selection-chip selection-chip--soft" },
    ...{ class: ({ 'selection-chip--active': __VLS_ctx.fieldDeduplicate }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "checkbox",
});
(__VLS_ctx.fieldDeduplicate);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "selection-chip selection-chip--soft" },
    ...{ class: ({ 'selection-chip--active': __VLS_ctx.fieldEnabled }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "checkbox",
});
(__VLS_ctx.fieldEnabled);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.fieldPatternExpr),
    rows: "3",
    placeholder: "适用于 PARTY_PATTERN / AMOUNT_PATTERN / DATE_KEYWORD",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.fieldKeywordConfig),
    rows: "3",
    placeholder: "支持换行、逗号或分号分隔",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.fieldDescription),
    rows: "2",
    placeholder: "说明该字段的业务用途",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.saveFieldDefinition) },
    ...{ class: "primary-btn" },
});
(__VLS_ctx.editingFieldId ? '更新字段定义' : '新增字段定义');
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-list field-list" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.fieldDefinitions))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (item.fieldDefinitionId),
        ...{ class: "item-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-top" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (item.fieldName);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
    (item.fieldCode);
    (item.extractorKind);
    (item.sortOrder);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status-pill" },
        'data-status': (item.enabled ? 'ENABLED' : 'DISABLED'),
    });
    (item.enabled ? 'ENABLED' : 'DISABLED');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
    (item.repeatable);
    (item.deduplicateByNormalized);
    (item.systemDefault);
    if (item.patternExpr) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "evidence" },
        });
        (item.patternExpr);
    }
    if (item.keywordConfig) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (item.keywordConfig);
    }
    if (item.description) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (item.description);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.beginEditField(item);
            } },
        ...{ class: "ghost-btn" },
        disabled: (item.systemDefault),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.toggleField(item);
            } },
        ...{ class: "ghost-btn" },
        disabled: (item.systemDefault),
    });
    (item.enabled ? '停用' : '启用');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.deleteField(item);
            } },
        ...{ class: "warn-btn" },
        disabled: (item.systemDefault),
    });
}
/** @type {__VLS_StyleScopedClasses['rule-page']} */ ;
/** @type {__VLS_StyleScopedClasses['top-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['console-select']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['console-select']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['item-list']} */ ;
/** @type {__VLS_StyleScopedClasses['item-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-top']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['evidence']} */ ;
/** @type {__VLS_StyleScopedClasses['item-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warn-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['field-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['console-select']} */ ;
/** @type {__VLS_StyleScopedClasses['field-switches']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-row']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip--soft']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip--soft']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip--soft']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['item-list']} */ ;
/** @type {__VLS_StyleScopedClasses['field-list']} */ ;
/** @type {__VLS_StyleScopedClasses['item-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-top']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['evidence']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['item-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warn-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            rules: rules,
            fieldDefinitions: fieldDefinitions,
            notice: notice,
            manualRuleName: manualRuleName,
            manualRuleCode: manualRuleCode,
            manualSource: manualSource,
            manualSeverity: manualSeverity,
            manualHitThreshold: manualHitThreshold,
            manualRuleContent: manualRuleContent,
            importTitle: importTitle,
            importRuleName: importRuleName,
            importRuleCode: importRuleCode,
            importSource: importSource,
            importSeverity: importSeverity,
            importHitThreshold: importHitThreshold,
            editingFieldId: editingFieldId,
            fieldCode: fieldCode,
            fieldName: fieldName,
            extractorKind: extractorKind,
            fieldPatternExpr: fieldPatternExpr,
            fieldKeywordConfig: fieldKeywordConfig,
            fieldRepeatable: fieldRepeatable,
            fieldDeduplicate: fieldDeduplicate,
            fieldEnabled: fieldEnabled,
            fieldSortOrder: fieldSortOrder,
            fieldDescription: fieldDescription,
            refreshAll: refreshAll,
            onSelectFile: onSelectFile,
            createManualRule: createManualRule,
            importRuleDocument: importRuleDocument,
            toggleRule: toggleRule,
            reindexRule: reindexRule,
            deleteRule: deleteRule,
            resetFieldForm: resetFieldForm,
            beginEditField: beginEditField,
            saveFieldDefinition: saveFieldDefinition,
            toggleField: toggleField,
            deleteField: deleteField,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
