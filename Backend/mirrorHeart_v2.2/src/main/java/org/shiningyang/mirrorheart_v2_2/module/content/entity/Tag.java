package org.shiningyang.mirrorheart_v2_2.module.content.entity;

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
 * 标签库
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("tag")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    @TableField("name")
    private String name;

    /**
     * 标签分组: mood(心情), genre(流派)...
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 被引用次数统计
     */
    @TableField("use_count")
    private Integer useCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
