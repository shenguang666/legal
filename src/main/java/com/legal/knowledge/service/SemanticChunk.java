package com.legal.knowledge.service;

import com.legal.enums.KbChunkType;

public record SemanticChunk(String content, KbChunkType chunkType, Integer parentGroup) {

    public static SemanticChunk normal(String content) {
        return new SemanticChunk(content, KbChunkType.NORMAL, null);
    }

    public static SemanticChunk parent(String content, int parentGroup) {
        return new SemanticChunk(content, KbChunkType.PARENT, parentGroup);
    }

    public static SemanticChunk child(String content, int parentGroup) {
        return new SemanticChunk(content, KbChunkType.CHILD, parentGroup);
    }
}
