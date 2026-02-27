package org.shiningyang.mirrorheart_v2_2.module.todo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("todo_recommendation")
public class TodoRecommendation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 推荐待办内容
     */
    private String text;
    
    /**
     * 状态: 0=下架/禁用, 1=启用
     */
    private Byte status;
    
    private LocalDateTime createdAt;
}