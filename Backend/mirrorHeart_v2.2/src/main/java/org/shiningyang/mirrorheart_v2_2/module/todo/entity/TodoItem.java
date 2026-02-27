package org.shiningyang.mirrorheart_v2_2.module.todo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户待办事项
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("todo_item")
public class TodoItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    /**
     * 0=未做, 1=已完成
     */
    @TableField("status")
    private Byte status;

    /**
     * 新增：完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
