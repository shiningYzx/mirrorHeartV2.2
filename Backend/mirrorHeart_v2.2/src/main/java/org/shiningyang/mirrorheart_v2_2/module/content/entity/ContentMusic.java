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
@TableName("content_music")
public class ContentMusic implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("content_id")
    private Long contentId;

    /**
     * 艺术家/歌手
     */
    @TableField("artist")
    private String artist;

    /**
     * 音乐文件链接
     */
    @TableField("audio_url")
    private String audioUrl;

    /**
     * 时长(毫秒)
     */
    @TableField("duration_ms")
    private Integer durationMs;

    /**
     * 歌词
     */
    @TableField("lyric")
    private String lyric;
}
