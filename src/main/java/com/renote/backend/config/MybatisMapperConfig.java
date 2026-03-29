package com.renote.backend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.renote.backend.mapper")
public class MybatisMapperConfig {
}
