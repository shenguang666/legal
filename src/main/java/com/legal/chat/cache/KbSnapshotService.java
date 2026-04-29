package com.legal.chat.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.mapper.KbDocumentMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;

/**
 * 知识快照版本服务：用于缓存 key 隔离。
 *
 * <p>简化实现：取租户下未删除文档的最大 docVersion，拼成 kbSnapshotVersion。</p>
 */
@Service
public class KbSnapshotService {

    private final KbDocumentMapper kbDocumentMapper;

    public KbSnapshotService(KbDocumentMapper kbDocumentMapper) {
        this.kbDocumentMapper = kbDocumentMapper;
    }

    public String getSnapshotVersion(Long tenantId) {
        if (tenantId == null) {
            return "dv0";
        }
        // 不引入自定义 SQL，直接取列表后求 max；数据量通常较小。
        return kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocumentEntity>()
                        .eq(KbDocumentEntity::getTenantId, tenantId)
                        .ne(KbDocumentEntity::getStatus, "DELETED")
                        .select(KbDocumentEntity::getDocVersion))
                .stream()
                .map(KbDocumentEntity::getDocVersion)
                .filter(v -> v != null)
                .max(Comparator.naturalOrder())
                .map(v -> "dv" + v)
                .orElse("dv0");
    }
}
