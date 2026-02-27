package org.shiningyang.mirrorheart_v2_2.module.todo.dto;

import lombok.Data;

@Data
public class TodoUpdateDto {
    // 标题，可选更新
    private String title;
    
    // 状态: 0=未完成, 1=已完成
    private Byte status;
}