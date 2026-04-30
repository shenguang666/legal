package com.legal.memory.service;

/**
 * memory_task_outbox 任务负载。
 */
public final class MemoryTaskPayloads {

    private MemoryTaskPayloads() {
    }

    public static class SummaryPayload {
        private Long assistantCount;

        public Long getAssistantCount() {
            return assistantCount;
        }

        public void setAssistantCount(Long assistantCount) {
            this.assistantCount = assistantCount;
        }
    }

    public static class QaPayload {
        private Long userMessageId;
        private Long assistantMessageId;
        private String question;
        private String answer;

        public Long getUserMessageId() {
            return userMessageId;
        }

        public void setUserMessageId(Long userMessageId) {
            this.userMessageId = userMessageId;
        }

        public Long getAssistantMessageId() {
            return assistantMessageId;
        }

        public void setAssistantMessageId(Long assistantMessageId) {
            this.assistantMessageId = assistantMessageId;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
