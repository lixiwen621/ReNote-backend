package com.renote.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新任务复习内容（富文本 HTML）；传空字符串表示清空（存库为 NULL）。
 */
@Data
public class UpdateTaskNoteContentRequest {

    @Size(max = 500000, message = "noteContent长度不能超过500000")
    private String noteContent;
}
