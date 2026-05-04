package com.legal.knowledge.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MinerU 结果包解析后的 Markdown 与图片资源集合。
 */
public record MineruParsePackage(
        String markdown,
        List<MineruImageEntry> images,
        List<MineruPackageFile> files,
        String markdownPath
) {

    public MineruParsePackage(String markdown, List<MineruImageEntry> images) {
        this(markdown, images, List.of(), null);
    }

    public MineruParsePackage {
        images = images == null ? List.of() : List.copyOf(images);
        files = files == null ? List.of() : List.copyOf(files);
    }

    public Map<String, MineruImageEntry> imagesByNormalizedPath() {
        return images.stream()
                .collect(Collectors.toMap(
                        image -> image.normalizedPath().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, ignored) -> left
                ));
    }

    public MineruImageEntry findImage(String markdownPath) {
        String normalized = MineruClient.normalizeZipPath(markdownPath);
        if (normalized == null) {
            return null;
        }
        return imagesByNormalizedPath().get(normalized.toLowerCase(Locale.ROOT));
    }
}
