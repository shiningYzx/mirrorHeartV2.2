package org.shiningyang.mirrorheart_v2_2.module.interaction.entity;

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
 * 用户关系表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-20
 */
@Getter
@Setter
@TableName("user_relation")
public class UserRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发起方
     */
    @TableField("from_user_id")
    private Long fromUserId;

    /**
     * 目标方
     */
    @TableField("to_user_id")
    private Long toUserId;

    /**
     * 1=关注(Follow), 2=拉黑(Block)
     */
    @TableField("type")
    private Byte type;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
