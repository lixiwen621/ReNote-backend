package com.renote.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 复习任务附件存储：本地磁盘或腾讯云 COS。
 */
@Data
@ConfigurationProperties(prefix = "review.attachment")
public class TaskAttachmentStorageProperties {

    /**
     * local：本地目录 + 静态映射；cos：腾讯云对象存储（生产推荐）。
     */
    private String storageBackend = "local";

    /** 本地模式：保存目录（相对进程工作目录或绝对路径） */
    private String uploadDir = "uploads/review-task";

    /** 本地模式：对外访问路径前缀 */
    private String publicBaseUrl = "/uploads/review-task";

    private int maxFileCount = 10;
    private long maxFileSizeBytes = 10L * 1024 * 1024;

    private Cos cos = new Cos();

    @Data
    public static class Cos {
        /** 如 ap-guangzhou、ap-beijing */
        private String region = "";
        private String bucket = "";
        private String secretId = "";
        private String secretKey = "";
        /** 对象键前缀，如 renote/review-task（不要首尾斜杠） */
        private String keyPrefix = "renote/review-task";
        /**
         * 访问 URL 前缀；不填则按官方域名拼接：https://{bucket}.cos.{region}.myqcloud.com
         * 若使用自定义域名或 CDN，填完整前缀（无末尾斜杠）。
         */
        private String publicBaseUrl = "";

        /**
         * 私有桶读附件时，接口返回的 {@code fileUrl} 使用预签名 URL 的有效期（秒），默认 1 小时。
         */
        private int presignedUrlExpirationSeconds = 3600;
    }
}
