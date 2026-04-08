package com.renote.backend.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.renote.backend.config.TaskAttachmentStorageProperties;
import com.renote.backend.entity.ReviewTaskAttachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.Date;

/**
 * 私有读 COS 桶场景下，为附件生成短期可访问的预签名 URL（GET）。
 *
 * @author tongguo.liu
 * @since 2026-04-08 12:00:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosAttachmentUrlService {

    private final TaskAttachmentStorageProperties storageProperties;
    private final ObjectProvider<COSClient> cosClientProvider;

    /**
     * 组装返回给前端的下载/预览地址：COS 且 {@code storage_path} 为 {@code cos://bucket/key} 时生成预签名 URL，否则返回库中 {@code file_url}。
     */
    public String publicUrlForResponse(ReviewTaskAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        if (!"cos".equalsIgnoreCase(storageProperties.getStorageBackend())) {
            return attachment.getFileUrl();
        }
        COSClient client = cosClientProvider.getIfAvailable();
        if (client == null) {
            return attachment.getFileUrl();
        }
        CosObjectRef ref = parseCosStoragePath(attachment.getStoragePath());
        if (ref == null) {
            return attachment.getFileUrl();
        }
        int seconds = Math.max(60, storageProperties.getCos().getPresignedUrlExpirationSeconds());
        try {
            Date expiration = new Date(System.currentTimeMillis() + seconds * 1000L);
            URL url = client.generatePresignedUrl(ref.bucket(), ref.key(), expiration, HttpMethodName.GET);
            return url.toString();
        } catch (Exception ex) {
            log.warn("生成 COS 预签名 URL 失败, attachmentId={}, bucket={}, key={}, err={}",
                    attachment.getId(), ref.bucket(), ref.key(), ex.getMessage());
            return attachment.getFileUrl();
        }
    }

    private static CosObjectRef parseCosStoragePath(String storagePath) {
        if (!StringUtils.hasText(storagePath) || !storagePath.startsWith("cos://")) {
            return null;
        }
        String rest = storagePath.substring("cos://".length());
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash >= rest.length() - 1) {
            return null;
        }
        String bucket = rest.substring(0, slash).trim();
        String key = rest.substring(slash + 1).trim();
        if (!StringUtils.hasText(bucket) || !StringUtils.hasText(key)) {
            return null;
        }
        return new CosObjectRef(bucket, key);
    }

    private record CosObjectRef(String bucket, String key) {
    }
}
