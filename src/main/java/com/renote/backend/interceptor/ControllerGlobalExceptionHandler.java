package com.renote.backend.interceptor;

import com.renote.backend.common.ApiResponse;
import com.renote.backend.common.I18nMessageException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@RestControllerAdvice
public class ControllerGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ControllerGlobalExceptionHandler.class);

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public ControllerGlobalExceptionHandler(MessageSource messageSource, LocaleResolver localeResolver) {
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @ExceptionHandler(I18nMessageException.class)
    public ApiResponse<Void> handleI18nMessage(I18nMessageException ex, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        String msg = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessageKey(), locale);
        if (ex.getCause() != null) {
            log.warn("I18nMessageException key={} resolved={} cause={}",
                    ex.getMessageKey(), msg, ex.getCause().toString());
        }
        return ApiResponse.fail(msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String error = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("参数校验失败");
        return ApiResponse.fail(error);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("未处理异常 [{}]", ex.getClass().getName(), ex);
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = ex.getClass().getSimpleName();
        }
        return ApiResponse.fail("系统异常: " + msg);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<Void> handleAuthenticationException(AuthenticationException ex) {
        return ApiResponse.fail("未登录或 token 无效");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException ex) {
        return ApiResponse.fail("无权限访问");
    }
}

