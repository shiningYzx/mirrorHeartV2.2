package org.shiningyang.mirrorheart_v2_2.module.content.entity;

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
@TableName("content_painting")
public class ContentPainting implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("content_id")
    private Long contentId;

    /**
     * 画家
     */
    @TableField("painter")
    private String painter;

    /**
     * 创作年份
     */
    @TableField("year")
    private String year;
}
