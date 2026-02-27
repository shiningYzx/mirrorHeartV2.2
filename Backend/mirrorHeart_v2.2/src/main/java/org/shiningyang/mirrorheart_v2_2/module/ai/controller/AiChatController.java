package org.shiningyang.mirrorheart_v2_2.module.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.ai.dto.AiChatRequestDto;
import org.shiningyang.mirrorheart_v2_2.module.ai.entity.AiChatMessage;
import org.shiningyang.mirrorheart_v2_2.module.ai.entity.AiChatSession;
import org.shiningyang.mirrorheart_v2_2.module.ai.service.IAiChatService;
import org.shiningyang.mirrorheart_v2_2.module.ai.mapper.AiChatMessageMapper;
import org.shiningyang.mirrorheart_v2_2.module.ai.mapper.AiChatSessionMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final IAiChatService aiChatService;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    /**
     * 流式对话接口 (SSE)
     * 注意：前端需使用支持 SSE 的 Fetch 请求或 EventSource 替代方案处理 POST
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody @Valid AiChatRequestDto dto) {
        // 🌟 核心：禁用所有的服务器和网络缓冲，让数据一产生就立马推给前端，解决没有打字效果的问题！
//        response.setHeader("Cache-Control", "no-cache, no-transform");
//        response.setHeader("X-Accel-Buffering", "no"); // 针对 Nginx 等代理服务器
//        response.setHeader("Connection", "keep-alive");

        Long userId = SecurityUtils.getUserId();
        // 设置跨域，防止前端收不到流
        return aiChatService.chatStream(userId, dto);
    }

    /**
     * 获取我的会话列表
     */
    @GetMapping("/session/list")
    public Result<List<AiChatSession>> getSessionList() {
        Long userId = SecurityUtils.getUserId();
        List<AiChatSession> list = sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .eq(AiChatSession::getIsDeleted, 0)
                .orderByDesc(AiChatSession::getUpdatedAt)); // 按最后更新时间排序
        return Result.success(list);
    }

    /**
     * 获取某个会话的历史消息记录
     */
    @GetMapping("/message/list")
    public Result<List<AiChatMessage>> getMessageHistory(@RequestParam Long sessionId) {
        Long userId = SecurityUtils.getUserId();
        
        // 校验越权
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error(403, "无权访问此会话");
        }

        List<AiChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getCreatedAt)); // 聊天记录按时间正序
        
        return Result.success(messages);
    }

    /**
     * 删除历史会话
     */
    @DeleteMapping("/session/{id}")
    public Result<String> deleteSession(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        aiChatService.deleteSession(userId, id);
        return Result.success("删除成功");
    }
}