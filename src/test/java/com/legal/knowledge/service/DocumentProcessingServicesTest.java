package com.legal.knowledge.service;

import com.legal.auth.mapper.LegalUserMapper;
import com.legal.common.AppException;
import com.legal.config.DocumentProcessingProperties;
import com.legal.enums.DocumentParseMethod;
import com.legal.enums.DocumentParseStatus;
import com.legal.enums.KbDocumentBizType;
import com.legal.enums.KbDocumentStatus;
import com.legal.enums.KbIndexStatus;
import com.legal.knowledge.entity.KbChunkEntity;
import com.legal.knowledge.entity.KbDocumentEntity;
import com.legal.knowledge.entity.KbDocumentParseTaskEntity;
import com.legal.knowledge.entity.KbIndexOutboxEntity;
import com.legal.knowledge.mapper.KbChunkImageRefMapper;
import com.legal.knowledge.mapper.KbChunkMapper;
import com.legal.knowledge.mapper.KbDocumentMapper;
import com.legal.knowledge.mapper.KbDocumentParseTaskMapper;
import com.legal.knowledge.mapper.KbIndexOutboxMapper;
import com.legal.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentProcessingServicesTest {

    @Test
    void capabilityShouldRejectUploadCountAboveConfiguredLimit() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.setMaxUploadDocuments(1);
        DocumentProcessingCapabilityService service = new DocumentProcessingCapabilityService(properties);

        assertThatThrownBy(() -> service.validateUploadCount(2))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("单次最多上传 1 个文档");
    }

    @Test
    void capabilityShouldResolveMineruOnlyWhenEnabledAndTokenConfigured() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getMineru().setEnabled(true);
        properties.getMineru().setApiToken("");
        DocumentProcessingCapabilityService unavailable = new DocumentProcessingCapabilityService(properties);

        assertThat(unavailable.resolveParseMethod(null)).isEqualTo(DocumentParseMethod.NATIVE);
        assertThat(unavailable.resolveParseMethod("MINERU_PRECISE")).isEqualTo(DocumentParseMethod.NATIVE);

        properties.getMineru().setApiToken("token");
        DocumentProcessingCapabilityService available = new DocumentProcessingCapabilityService(properties);

        assertThat(available.resolveParseMethod(null)).isEqualTo(DocumentParseMethod.MINERU_PRECISE);
        assertThat(available.resolveParseMethod("MINERU_PRECISE")).isEqualTo(DocumentParseMethod.MINERU_PRECISE);
        assertThat(available.availableParseMethods()).containsExactly(DocumentParseMethod.NATIVE, DocumentParseMethod.MINERU_PRECISE);
    }

    @Test
    void importShouldKeepNativeParsingCompatibleAndCompleteDocument() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        DocumentProcessingCapabilityService capabilityService = new DocumentProcessingCapabilityService(new DocumentProcessingProperties());
        NativeDocumentParser nativeDocumentParser = mock(NativeDocumentParser.class);
        DocumentParseTaskService parseTaskService = mock(DocumentParseTaskService.class);
        MineruImageAssetStorageService storageService = mock(MineruImageAssetStorageService.class);
        DocumentImportService service = new DocumentImportService(documentMapper, capabilityService, nativeDocumentParser, parseTaskService, storageService, mock(LegalUserMapper.class));
        MockMultipartFile file = new MockMultipartFile("file", "contract.txt", "text/plain", "合同内容".getBytes(StandardCharsets.UTF_8));
        when(nativeDocumentParser.parse(any(DocumentParseRequest.class)))
                .thenReturn(new DocumentParseResult(DocumentParseMethod.NATIVE, "合同内容", null, List.of("合同内容"), null, null, null, null));

        KbDocumentEntity document = service.importDocument(principal(), file, null, null, KbDocumentBizType.KNOWLEDGE, null, null, null, false, "内容过短");

        assertThat(document.getParseMethod()).isEqualTo(DocumentParseMethod.NATIVE);
        assertThat(document.getParseStatus()).isEqualTo(DocumentParseStatus.PROCESSING);
        assertThat(document.getStatus()).isEqualTo(KbDocumentStatus.PROCESSING);
        verify(documentMapper).insert(document);
        verify(parseTaskService).completeNative(eq(document), eq(List.of("合同内容")), isNull(), isNull());
        verify(parseTaskService, never()).createTask(any(), any(), any());
    }

    @Test
    void importShouldCreateMineruPendingTaskWhenMineruAvailable() {
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getMineru().setEnabled(true);
        properties.getMineru().setApiToken("token");
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        NativeDocumentParser nativeDocumentParser = mock(NativeDocumentParser.class);
        DocumentParseTaskService parseTaskService = mock(DocumentParseTaskService.class);
        DocumentImportService service = new DocumentImportService(documentMapper, new DocumentProcessingCapabilityService(properties), nativeDocumentParser, parseTaskService, mock(MineruImageAssetStorageService.class), mock(LegalUserMapper.class));
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "PDF".getBytes(StandardCharsets.UTF_8));

        KbDocumentEntity document = service.importDocument(principal(), file, "精准合同", "上传", KbDocumentBizType.KNOWLEDGE, "MINERU_PRECISE", null, null, false, "内容过短");

        assertThat(document.getParseMethod()).isEqualTo(DocumentParseMethod.MINERU_PRECISE);
        assertThat(document.getParseStatus()).isEqualTo(DocumentParseStatus.PENDING);
        assertThat(document.getStatus()).isEqualTo(KbDocumentStatus.PENDING);
        verify(documentMapper).insert(document);
        verify(nativeDocumentParser, never()).parse(any());
        verify(parseTaskService).createTask(eq(document), eq("contract.pdf"), eq("PDF".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void mineruTaskShouldCompleteDocumentWhenExtractionSucceeds() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        KbChunkMapper chunkMapper = mock(KbChunkMapper.class);
        KbChunkImageRefMapper chunkImageRefMapper = mock(KbChunkImageRefMapper.class);
        KbIndexOutboxMapper outboxMapper = mock(KbIndexOutboxMapper.class);
        KbDocumentParseTaskMapper taskMapper = mock(KbDocumentParseTaskMapper.class);
        MineruClient mineruClient = mock(MineruClient.class);
        SemanticDocumentChunker semanticDocumentChunker = mock(SemanticDocumentChunker.class);
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        properties.getMineru().setChunkSize(456);
        properties.getMineru().setMinChunkSize(123);
        MineruImageAssetStorageService storageService = mock(MineruImageAssetStorageService.class);
        DocumentParseTaskService service = new DocumentParseTaskService(documentMapper, chunkMapper, chunkImageRefMapper, outboxMapper, taskMapper, mock(NativeDocumentParser.class), mineruClient, semanticDocumentChunker, new DocumentContentCleaner(properties), mock(DocumentCleaningLogService.class), mock(MineruImageAssetProcessor.class), storageService, properties);
        KbDocumentEntity document = mineruDocument(KbDocumentBizType.KNOWLEDGE);
        KbDocumentParseTaskEntity task = mineruTask();
        MineruParsePackage parsePackage = new MineruParsePackage("# 合同\n\n第一条 内容", List.of());
        when(documentMapper.selectOne(any())).thenReturn(document);
        when(mineruClient.submit("contract.pdf", task.getFileContent())).thenReturn(new MineruClient.MineruUploadSession("batch-1", "data-1"));
        when(mineruClient.waitForResult("batch-1", "data-1")).thenReturn(MineruClient.MineruExtractResult.done("https://example.test/full.zip"));
        when(mineruClient.downloadPackage("https://example.test/full.zip")).thenReturn(parsePackage);
        when(storageService.uploadPackage(document, parsePackage, "contract.pdf", task.getFileContent())).thenReturn(new MineruPackageUploadResult("bucket", "prefix", "url", "# 合同\n\n第一条 内容"));
        when(semanticDocumentChunker.chunkMarkdownStructured("# 合同\n\n第一条 内容", 456, 123)).thenReturn(List.of(SemanticChunk.normal("# 合同\n第一条 内容")));

        service.processTask(task);

        assertThat(document.getParseStatus()).isEqualTo(DocumentParseStatus.COMPLETED);
        assertThat(document.getMineruBatchId()).isEqualTo("batch-1");
        assertThat(document.getMineruDataId()).isEqualTo("data-1");
        assertThat(document.getMineruFullZipUrl()).isEqualTo("https://example.test/full.zip");
        verify(chunkMapper).insert(any(KbChunkEntity.class));
        verify(outboxMapper).insert(any(KbIndexOutboxEntity.class));
        verify(semanticDocumentChunker).chunkMarkdownStructured("# 合同\n\n第一条 内容", 456, 123);
        verify(taskMapper).markCompleted(100L);
    }

    @Test
    void mineruTaskShouldMarkFailureWhenExtractionFails() {
        KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
        KbDocumentParseTaskMapper taskMapper = mock(KbDocumentParseTaskMapper.class);
        MineruClient mineruClient = mock(MineruClient.class);
        DocumentProcessingProperties properties = new DocumentProcessingProperties();
        DocumentParseTaskService service = new DocumentParseTaskService(documentMapper, mock(KbChunkMapper.class), mock(KbChunkImageRefMapper.class), mock(KbIndexOutboxMapper.class), taskMapper, mock(NativeDocumentParser.class), mineruClient, mock(SemanticDocumentChunker.class), new DocumentContentCleaner(properties), mock(DocumentCleaningLogService.class), mock(MineruImageAssetProcessor.class), mock(MineruImageAssetStorageService.class), properties);
        KbDocumentEntity document = mineruDocument(KbDocumentBizType.KNOWLEDGE);
        KbDocumentParseTaskEntity task = mineruTask();
        when(documentMapper.selectOne(any())).thenReturn(document);
        when(mineruClient.submit("contract.pdf", task.getFileContent())).thenThrow(AppException.badRequest("MinerU 限流"));

        service.processTask(task);

        assertThat(document.getParseStatus()).isEqualTo(DocumentParseStatus.FAILED);
        assertThat(document.getParseFailureReason()).isEqualTo("MinerU 限流");
        verify(taskMapper).markFailed(eq(100L), eq("MinerU 限流"), eq(5L));
        verify(documentMapper).updateById(document);
    }

    private AuthPrincipal principal() {
        return new AuthPrincipal(1L, 2L, "ADMIN");
    }

    private KbDocumentEntity mineruDocument(KbDocumentBizType bizType) {
        KbDocumentEntity document = new KbDocumentEntity();
        document.setDocumentId(10L);
        document.setTenantId(1L);
        document.setOwnerUserId(2L);
        document.setBizType(bizType);
        document.setStatus(KbDocumentStatus.PENDING);
        document.setIndexStatus(KbIndexStatus.PENDING);
        document.setDocVersion(1);
        document.setParseMethod(DocumentParseMethod.MINERU_PRECISE);
        document.setParseStatus(DocumentParseStatus.PENDING);
        return document;
    }

    private KbDocumentParseTaskEntity mineruTask() {
        KbDocumentParseTaskEntity task = new KbDocumentParseTaskEntity();
        task.setTaskId(100L);
        task.setTenantId(1L);
        task.setDocumentId(10L);
        task.setDocVersion(1);
        task.setParseMethod(DocumentParseMethod.MINERU_PRECISE);
        task.setParseStatus(DocumentParseStatus.PENDING);
        task.setFileName("contract.pdf");
        task.setFileContent("PDF".getBytes(StandardCharsets.UTF_8));
        task.setRetryCount(0);
        return task;
    }
}