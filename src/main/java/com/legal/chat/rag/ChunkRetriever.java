package com.legal.chat.rag;

import java.util.List;

public interface ChunkRetriever {

    List<RetrievedChunk> retrieve(Long tenantId, String question, int topK);
}
