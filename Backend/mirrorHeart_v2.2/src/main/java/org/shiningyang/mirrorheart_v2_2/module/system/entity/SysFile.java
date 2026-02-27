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
 * 文件资源管理表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("sys_file")
public class SysFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 上传者
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 文件访问地址
     */
    @TableField("url")
    private String url;

    /**
     * OSS存储Key
     */
    @TableField("file_key")
    private String fileKey;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("mime_type")
    private String mimeType;

    /**
     * 场景: AVATAR/POST_IMG/AUDIO
     */
    @TableField("scene")
    private String scene;

    /**
     * 0=临时(未引用), 1=已引用(持久化)
     */
    @TableField("status")
    private Byte status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
