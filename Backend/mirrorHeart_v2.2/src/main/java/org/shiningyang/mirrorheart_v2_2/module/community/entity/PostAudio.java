package org.shiningyang.mirrorheart_v2_2.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("post_audio")
public class PostAudio implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    /**
     * 音频地址
     */
    @TableField("url")
    private String url;

    /**
     * 时长
     */
    @TableField("duration_ms")
    private Integer durationMs;
}
