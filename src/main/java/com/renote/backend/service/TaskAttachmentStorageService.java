package com.renote.backend.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.renote.backend.config.TaskAttachmentStorageProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class TaskAttachmentStorageService {

    private final TaskAttachmentStorageProperties storageProperties;
    private final ObjectProvider<COSClient> cosClientProvider;

    public TaskAttachmentStorageService(
            TaskAttachmentStorageProperties storageProperties,
            ObjectProvider<COSClient> cosClientProvider) {
        this.storageProperties = storageProperties;
        this.cosClientProvider = cosClientProvider;
    }

    public StoredAttachment save(MultipartFile file, Long userId, Long taskId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if ("cos".equalsIgnoreCase(storageProperties.getStorageBackend())) {
            COSClient client = cosClientProvider.getIfAvailable();
            if (client == null) {
                throw new IllegalStateException("已启用 COS 但未注入 COSClient，请检查 review.attachment.cos 配置与 CosClientConfiguration");
            }
            return saveToCos(client, file, userId, taskId);
        }
        return saveToLocal(file, userId, taskId);
    }

    private StoredAttachment saveToCos(COSClient cosClient, MultipartFile file, Long userId, Long taskId) {
        TaskAttachmentStorageProperties.Cos cos = storageProperties.getCos();
        String bucket = cos.getBucket().trim();
        String originalName = resolveOriginalFileName(file);
        String extension = extensionOf(originalName);
        String storedName = UUID.randomUUID() + extension;
        String key = buildObjectKey(cos.getKeyPrefix(), userId, taskId, storedName);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        if (StringUtils.hasText(file.getContentType())) {
            metadata.setContentType(file.getContentType());
        }
        try (InputStream in = file.getInputStream()) {
            PutObjectRequest req = new PutObjectRequest(bucket, key, in, metadata);
            cosClient.putObject(req);
        } catch (IOException ex) {
            throw new IllegalStateException("上传到 COS 失败: " + originalName, ex);
        }

        String fileUrl = buildCosPublicUrl(cos, bucket, key);
        return new StoredAttachment(storedName, "cos://" + bucket + "/" + key, fileUrl);
    }

    private static String buildCosPublicUrl(TaskAttachmentStorageProperties.Cos cos, String bucket, String key) {
        if (StringUtils.hasText(cos.getPublicBaseUrl())) {
            return trimTrailingSlash(cos.getPublicBaseUrl().trim()) + "/" + key;
        }
        String region = cos.getRegion().trim();
        return "https://" + bucket + ".cos." + region + ".myqcloud.com/" + key;
    }

    private StoredAttachment saveToLocal(MultipartFile file, Long userId, Long taskId) {
        String originalName = resolveOriginalFileName(file);
        String extension = extensionOf(originalName);
        String storedName = UUID.randomUUID() + extension;
        Path dir = Paths.get(storageProperties.getUploadDir(), String.valueOf(userId), String.valueOf(taskId));
        Path destination = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("保存附件失败: " + originalName, ex);
        }

        String baseUrl = trimTrailingSlash(storageProperties.getPublicBaseUrl());
        String fileUrl = baseUrl + "/" + userId + "/" + taskId + "/" + storedName;
        return new StoredAttachment(storedName, destination.toString(), fileUrl);
    }

    private static String resolveOriginalFileName(MultipartFile file) {
        return StringUtils.hasText(file.getOriginalFilename())
                ? Paths.get(file.getOriginalFilename()).getFileName().toString()
                : "unknown.bin";
    }

    private static String extensionOf(String originalName) {
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx > -1) {
            return originalName.substring(dotIdx);
        }
        return "";
    }

    private static String buildObjectKey(String keyPrefix, Long userId, Long taskId, String storedName) {
        String prefix = keyPrefix == null ? "" : keyPrefix.trim();
        prefix = prefix.replaceAll("^/+", "").replaceAll("/+$", "");
        if (!prefix.isEmpty()) {
            return prefix + "/" + userId + "/" + taskId + "/" + storedName;
        }
        return userId + "/" + taskId + "/" + storedName;
    }

    private static String trimTrailingSlash(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        return input.endsWith("/") ? input.substring(0, input.length() - 1) : input;
    }

    public record StoredAttachment(String storedFileName, String storagePath, String fileUrl) {
    }
}
