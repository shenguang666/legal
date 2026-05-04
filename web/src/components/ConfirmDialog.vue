<template>
  <Teleport to="body">
    <div v-if="modelValue" class="confirm-panel" role="dialog" aria-modal="true" :aria-labelledby="titleId">
      <div class="confirm-modal card">
        <div class="confirm-head">
          <div>
            <p class="tag">{{ eyebrow }}</p>
            <h3 :id="titleId">{{ title }}</h3>
          </div>
          <button class="ghost-btn" type="button" @click="emit('cancel')">关闭</button>
        </div>
        <p class="confirm-message">{{ message }}</p>
        <div v-if="targetName" class="confirm-target">
          <span>{{ targetLabel }}</span>
          <strong>{{ targetName }}</strong>
        </div>
        <div class="confirm-actions">
          <button class="ghost-btn" type="button" @click="emit('cancel')">取消</button>
          <button :class="danger ? 'warn-btn' : 'primary-btn'" type="button" @click="emit('confirm')">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: boolean;
    title: string;
    message: string;
    targetName?: string;
    targetLabel?: string;
    confirmText?: string;
    eyebrow?: string;
    danger?: boolean;
  }>(),
  {
    targetName: '',
    targetLabel: '对象',
    confirmText: '确认',
    eyebrow: '操作确认',
    danger: false,
  },
);

const emit = defineEmits<{
  cancel: [];
  confirm: [];
}>();

const titleId = `confirm-${Math.random().toString(36).slice(2)}`;
</script>

<style scoped>
.confirm-panel {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgba(19, 28, 23, 0.46);
  backdrop-filter: blur(6px);
}

.confirm-modal {
  width: min(520px, 100%);
  padding: 1rem;
}

.confirm-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.confirm-head h3 {
  margin: 0;
}

.confirm-message {
  margin: 0.85rem 0 0;
  color: var(--ink-soft);
  line-height: 1.65;
}

.confirm-target {
  display: grid;
  gap: 0.35rem;
  margin-top: 0.85rem;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.5);
  padding: 0.75rem;
}

.confirm-target span {
  color: var(--ink-soft);
  font-size: 0.82rem;
}

.confirm-target strong {
  color: var(--ink);
  overflow-wrap: anywhere;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.55rem;
  margin-top: 1rem;
}
</style>
