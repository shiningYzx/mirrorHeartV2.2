package org.shiningyang.mirrorheart_v2_2.module.system.entity;

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
 * 用户搜索历史
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("search_history")
public class SearchHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 搜索关键词
     */
    @TableField("keyword")
    private String keyword;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
