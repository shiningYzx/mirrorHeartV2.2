package org.shiningyang.mirrorheart_v2_2.module.ai.service;

import org.shiningyang.mirrorheart_v2_2.module.ai.dto.AiChatRequestDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IAiChatService {
    /**
     * 发送消息并返回 SSE 流
     */
    SseEmitter chatStream(Long userId, AiChatRequestDto requestDto);

    /**
     * 删除历史会话
     */
    void deleteSession(Long userId, Long sessionId);
}