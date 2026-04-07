package com.renote.backend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 仅在 {@code review.attachment.storage-backend=cos} 时创建 COS 客户端。
 */
@Configuration
@ConditionalOnProperty(prefix = "review.attachment", name = "storage-backend", havingValue = "cos")
public class CosClientConfiguration {

    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient(TaskAttachmentStorageProperties properties) {
        TaskAttachmentStorageProperties.Cos c = properties.getCos();
        if (!StringUtils.hasText(c.getRegion())
                || !StringUtils.hasText(c.getBucket())
                || !StringUtils.hasText(c.getSecretId())
                || !StringUtils.hasText(c.getSecretKey())) {
            throw new IllegalStateException(
                    "review.attachment.cos 配置不完整：需要 region、bucket、secret-id、secret-key（建议通过环境变量注入）");
        }
        COSCredentials cred = new BasicCOSCredentials(c.getSecretId().trim(), c.getSecretKey().trim());
        ClientConfig clientConfig = new ClientConfig(new Region(c.getRegion().trim()));
        return new COSClient(cred, clientConfig);
    }
}
