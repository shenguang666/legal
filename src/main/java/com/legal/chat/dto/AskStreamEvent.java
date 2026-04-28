package com.legal.chat.dto;

/**
 * SSE 流式问答事件。
 * type: thinking | answer | citations | done | error
 */
public class AskStreamEvent {

    private String type;
    private String data;

    public AskStreamEvent() {
    }

    public AskStreamEvent(String type, String data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
