package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContentDetailVo extends ContentListVo {
    // 核心设计：使用 Object 存储子表数据
    // 如果是金句，这里放 ContentQuote 对象
    // 如果是书籍，这里放 ContentBook 对象
    private Object specificData; 
}