package com.renote.backend.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * MyBatis 拦截器：对 SQL 或参数中的可疑内容做基本注入检测。
 * 注意：MyBatis 使用参数占位符时，绝大多数注入不会出现在 SQL 字符串里，
 * 该拦截器主要用于拦截动态拼接/不规范拼接导致的高风险输入。
 */
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlInjectionInterceptor implements Interceptor {

    /**
     * 尽量“保守”地检测高风险 SQL 注入片段。
     * 注意：不要把 SELECT/INSERT/UPDATE 这类关键字当作可疑，
     * 否则所有正常查询都会被拦截。
     */
    private static final Pattern SUSPICIOUS_SQL_PATTERN = Pattern.compile(
            "(\\bUNION\\b|\\bDROP\\b|\\b--\\b|/\\*|\\*/|;|\\bOR\\b\\s+1\\s*=\\s*1|\\bOR\\b\\s+'[^']*'\\s*=\\s*'[^']*')",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${security.sql-injection.enabled:true}")
    private boolean enabled;

    @Value("${security.sql-injection.block:true}")
    private boolean block;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!enabled) {
            return invocation.proceed();
        }

        Object target = invocation.getTarget();
        if (!(target instanceof StatementHandler)) {
            return invocation.proceed();
        }
        StatementHandler statementHandler = (StatementHandler) target;

        BoundSql boundSql = extractBoundSql(statementHandler);
        String sql = boundSql != null ? boundSql.getSql() : null;
        Object parameterObject = boundSql != null ? boundSql.getParameterObject() : null;

        boolean suspicious = false;
        if (sql != null && SUSPICIOUS_SQL_PATTERN.matcher(sql).find()) {
            suspicious = true;
        }

        if (!suspicious && parameterObject != null) {
            suspicious = containsSuspiciousString(parameterObject);
        }

        if (suspicious && block) {
            throw new IllegalArgumentException("疑似 SQL 注入/不安全参数，请检查输入");
        }
        return invocation.proceed();
    }

    private BoundSql extractBoundSql(StatementHandler statementHandler) {
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        Object value = tryGet(metaObject, "delegate.boundSql");
        if (value instanceof BoundSql) {
            return (BoundSql) value;
        }
        value = tryGet(metaObject, "boundSql");
        if (value instanceof BoundSql) {
            return (BoundSql) value;
        }
        return null;
    }

    private Object tryGet(MetaObject metaObject, String path) {
        try {
            return metaObject.getValue(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean containsSuspiciousString(Object parameterObject) {
        if (parameterObject instanceof String) {
            String s = (String) parameterObject;
            return SUSPICIOUS_SQL_PATTERN.matcher(s).find();
        }
        if (parameterObject instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parameterObject;
            for (Object v : map.values()) {
                if (v instanceof String) {
                    String str = (String) v;
                    if (SUSPICIOUS_SQL_PATTERN.matcher(str).find()) {
                    return true;
                    }
                }
            }
            return false;
        }
        // POJO：扫描所有 String 字段
        Field[] fields = parameterObject.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (!String.class.equals(f.getType())) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object v = f.get(parameterObject);
                if (v instanceof String) {
                    String str = (String) v;
                    if (SUSPICIOUS_SQL_PATTERN.matcher(str).find()) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return false;
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(java.util.Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}

