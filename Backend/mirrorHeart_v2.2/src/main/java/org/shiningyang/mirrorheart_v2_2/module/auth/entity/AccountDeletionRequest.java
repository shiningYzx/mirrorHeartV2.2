package org.shiningyang.mirrorheart_v2_2.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_deletion_request")
public class AccountDeletionRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private LocalDateTime requestedAt;
    private LocalDateTime executeAfter;
    private LocalDateTime canceledAt;
    private LocalDateTime executedAt;
    private String reason;
}