package com.renote.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新任务笔记链接；传空字符串表示清空链接（存库为 NULL）。
 */
@Data
public class UpdateTaskNoteUrlRequest {

    @Size(max = 1024, message = "noteUrl长度不能超过1024")
    private String noteUrl;
}
