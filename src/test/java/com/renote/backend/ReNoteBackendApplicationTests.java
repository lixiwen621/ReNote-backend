package com.renote.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 完整 Spring 上下文依赖 MySQL/MyBatis 及各类 Bean，在 CI/无库环境下会失败。
 * Controller/Service 层已有切片测试与 Mockito 单测；需要整体验证时可本地起 MySQL 后去掉 @Disabled。
 */
@SpringBootTest
@Disabled("完整上下文需数据库，默认跳过；打包请使用 mvn package 或 mvn -DskipTests package")
class ReNoteBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
