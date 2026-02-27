package org.shiningyang.mirrorheart_v2_2.module.ai.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.utils.SparkAuthUtil;
import org.shiningyang.mirrorheart_v2_2.module.ai.dto.AiChatRequestDto;
import org.shiningyang.mirrorheart_v2_2.module.ai.entity.AiChatMessage;
import org.shiningyang.mirrorheart_v2_2.module.ai.entity.AiChatSession;
import org.shiningyang.mirrorheart_v2_2.module.ai.mapper.AiChatMessageMapper;
import org.shiningyang.mirrorheart_v2_2.module.ai.mapper.AiChatSessionMapper;
import org.shiningyang.mirrorheart_v2_2.module.ai.service.IAiChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    // 🌟 新增：定义 CBT 心理教练的 System Prompt (使用 Java 15+ 的多行文本块语法)
    private static final String SYSTEM_PROMPT = """
            # Role
            你是一位专业的 CBT（认知行为疗法）心理教练，名为“Mirror”。你的目标是引导用户识别并重塑他们的“自动思维”。
            # Style
            - 语气：温和、坚定、具有同理心。
            - 原则：每轮对话只提一个问题，避免说教，引导用户自我发现。
            - 禁忌：严禁使用“你要坚强”、“别想太多”等无效安慰。
            # Logic Workflow (严格遵循以下阶段)
            ## Phase 1: 情绪锚定 (Emotion Labeling)
            - 动作：认可用户的情绪。
            - 提问：引导用户描述当下的感受及其强度（0-10分）。
            - 示例：“听起来这件事让你感到很受挫。如果痛苦有分值，现在你心里的分值是多少？”
            ## Phase 2: 捕捉“热想法” (Catching Hot Thoughts)
            - 动作：识别语音/文字中的认知偏差。
            - 提问：引导用户写出那一刻脑子里最极端的那句话。
            - 示例：“你刚才提到‘面试失败说明我不行’，这句话在脑子里闪过时，你的第一反应是什么？”
            ## Phase 3: 证据辩论 (Evidence Challenging)
            - 动作：苏格拉底式提问。
            - 指令：引导用户寻找支持和反驳该想法的客观事实。
            - 示例：“我们来做个实验，除了‘我不行’这个解释，还有什么客观因素可能导致了这次失败？”
            ## Phase 4: 思维重塑 (Cognitive Restructuring)
            - 动作：生成替代性思维。
            - 示例：“基于刚才的分析，我们能不能试着写出一个更符合客观事实的新想法？”
            # Emergency Protocol
            如果检测到用户有自残、自杀等极端负面词汇，立即停止 CBT 引导，输出：
            “我感觉到你现在处于极大的痛苦中，这超出了我的处理能力。请拨打心理援助热线：400-161-9995（24小时希望热线），或者寻求身边人的帮助。你很重要，请不要独自面对。”
            """;

    @Value("${xfyun.spark.app-id}")
    private String appId;
    @Value("${xfyun.spark.api-key}")
    private String apiKey;
    @Value("${xfyun.spark.api-secret}")
    private String apiSecret;
    @Value("${xfyun.spark.host-url}")
    private String hostUrl;
    @Value("${xfyun.spark.domain}")
    private String domain;

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new CustomException("会话不存在或无权操作");
        }
        // 逻辑删除该会话
        sessionMapper.deleteById(sessionId);

        // 如果想连同消息记录一起物理删除，可以放开下面这行：
        // messageMapper.delete(new LambdaQueryWrapper<AiChatMessage>().eq(AiChatMessage::getSessionId, sessionId));
    }


    @Override
    public SseEmitter chatStream(Long userId, AiChatRequestDto requestDto) {
        SseEmitter emitter = new SseEmitter(0L); // 0表示永不超时，保持长连接

        // 1. 处理会话 (Session)
        Long sessionId = requestDto.getSessionId();
        if (sessionId == null) {
            // 新建会话
            AiChatSession session = new AiChatSession();
            session.setUserId(userId);
            // 默认取用户第一句话的前10个字作为标题
            session.setTitle(requestDto.getContent().length() > 10 ?
                    requestDto.getContent().substring(0, 10) + "..." : requestDto.getContent());
            session.setIsDeleted((byte) 0);
            sessionMapper.insert(session);
            sessionId = session.getId();

            // 将新生成的 sessionId 通过 SSE 首先发给前端，方便前端记录
            sendSseMessage(emitter, "session_id", sessionId.toString());
        } else {
            // 校验会话归属
            AiChatSession existSession = sessionMapper.selectById(sessionId);
            if (existSession == null || !existSession.getUserId().equals(userId)) {
                throw new CustomException("会话不存在或无权访问");
            }
        }

        final Long currentSessionId = sessionId;

        // 2. 将用户的问题入库
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSessionId(currentSessionId);
        userMsg.setRole("user");
        userMsg.setContent(requestDto.getContent());
        messageMapper.insert(userMsg);

        // 3. 组装发给星火大模型的 JSON 请求体 (携带历史记录)
        String requestJson = buildSparkRequest(currentSessionId, requestDto.getThinking());

        // 4. 连接 WebSocket 并处理流
        try {
            String authUrl = SparkAuthUtil.getAuthUrl(hostUrl, apiKey, apiSecret);

            HttpClient client = HttpClient.newHttpClient();
            client.newWebSocketBuilder()
                    .buildAsync(URI.create(authUrl), new SparkWebSocketListener(emitter, currentSessionId, messageMapper))
                    .thenAccept(ws -> {
                        // 连接成功后发送请求 JSON
                        ws.sendText(requestJson, true);
                    }).exceptionally(ex -> {
                        log.error("星火WebSocket连接失败", ex);
                        emitter.completeWithError(ex);
                        return null;
                    });

        } catch (Exception e) {
            log.error("AI 聊天初始化失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 组装星火的请求体，包含历史上下文
     */
    private String buildSparkRequest(Long sessionId, String thinkingMode) {
        JSONObject request = new JSONObject();

        // 1. Header
        JSONObject header = new JSONObject();
        header.set("app_id", appId);
        header.set("uid", sessionId.toString()); // 用 sessionId 作为 uid
        request.set("header", header);

        // 2. Parameter
        JSONObject parameter = new JSONObject();
        JSONObject chat = new JSONObject();
        chat.set("domain", domain);
        chat.set("max_tokens", 8192); // 合理的 token 限制
        JSONObject thinking = new JSONObject();
        thinking.set("type", thinkingMode); // enabled, disabled, auto
        chat.set("thinking", thinking);
        parameter.set("chat", chat);
        request.set("parameter", parameter);

        // 3. Payload (查询历史聊天记录)
        JSONObject payload = new JSONObject();
        JSONObject message = new JSONObject();
        JSONArray textArray = new JSONArray();

        // 🌟 核心修改：在对话数组的最开头，强制注入 System Prompt
        JSONObject systemObj = new JSONObject();
        systemObj.set("role", "system");
        systemObj.set("content", SYSTEM_PROMPT);
        textArray.add(systemObj);

        // 获取历史记录 (限制最近 10 条，避免 token 超限)
        List<AiChatMessage> history = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getCreatedAt)
                .last("LIMIT 10"));

        // 反转顺序：将最新的放在最后面 (按时间正序)
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatMessage msg = history.get(i);
            JSONObject textObj = new JSONObject();
            textObj.set("role", msg.getRole());
            textObj.set("content", msg.getContent());
            textArray.add(textObj);
        }

        message.set("text", textArray);
        payload.set("message", message);
        request.set("payload", payload);

        return request.toString();
    }

    /**
     * 辅助发送 SSE 消息
     */
    private void sendSseMessage(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.warn("SSE 推送失败 (用户可能已断开): {}", e.getMessage());
        }
    }

    /**
     * WebSocket 监听器内部类，负责接收大模型消息 -> 转发 SSE -> 最终存库
     */
    class SparkWebSocketListener implements WebSocket.Listener {
        private final SseEmitter emitter;
        private final Long sessionId;
        private final AiChatMessageMapper messageMapper;

        // 拼接大模型返回的文本，用于最终入库
        private final StringBuilder fullContent = new StringBuilder();
        private final StringBuilder fullReasoning = new StringBuilder();

        public SparkWebSocketListener(SseEmitter emitter, Long sessionId, AiChatMessageMapper messageMapper) {
            this.emitter = emitter;
            this.sessionId = sessionId;
            this.messageMapper = messageMapper;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                JSONObject response = JSONUtil.parseObj(data.toString());
                JSONObject header = response.getJSONObject("header");
                int code = header.getInt("code");

                if (code != 0) {
                    log.error("星火接口返回错误: {}", response);
                    sendSseMessage(emitter, "error", "大模型接口异常: " + header.getStr("message"));
                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "error");
                    emitter.complete();
                    return CompletableFuture.completedFuture(null);
                }

                JSONObject payload = response.getJSONObject("payload");
                if (payload != null && payload.containsKey("choices")) {
                    JSONObject choices = payload.getJSONObject("choices");
                    JSONArray textArray = choices.getJSONArray("text");
                    if (textArray != null && !textArray.isEmpty()) {
                        JSONObject textObj = textArray.getJSONObject(0);

                        // X2 模型：深度思考的内容在 reasoning_content，最终回复在 content
                        String reasoningContent = textObj.getStr("reasoning_content");
                        String content = textObj.getStr("content");

                        if (reasoningContent != null && !reasoningContent.isEmpty()) {
                            fullReasoning.append(reasoningContent);
                            // 将思考过程打上标记发送给前端
                            sendSseMessage(emitter, "reasoning", reasoningContent);
                        }

                        if (content != null && !content.isEmpty()) {
                            fullContent.append(content);
                            // 将正常内容发送给前端
                            sendSseMessage(emitter, "content", content);
                        }
                    }

                    // status == 2 表示最后一条消息
                    int status = choices.getInt("status");
                    if (status == 2) {
                        saveAssistantMessage();
                        sendSseMessage(emitter, "done", "[DONE]");
                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
                        emitter.complete();
                    }
                }
            } catch (Exception e) {
                log.error("解析模型响应失败", e);
                emitter.completeWithError(e);
            }
            // 必须请求下一条消息
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("WebSocket 连接异常", error);
            sendSseMessage(emitter, "error", "网络连接异常");
            emitter.completeWithError(error);
        }

        /**
         * 结束时将 AI 的完整回复存入数据库
         */
        private void saveAssistantMessage() {
            AiChatMessage aiMsg = new AiChatMessage();
            aiMsg.setSessionId(sessionId);
            aiMsg.setRole("assistant");

            // 组装最终存储的文本：保留深度思考过程，类似于 <think>...</think>
            String finalStoreText = fullContent.toString();
            if (fullReasoning.length() > 0) {
                finalStoreText = "<think>\n" + fullReasoning.toString() + "\n</think>\n" + finalStoreText;
            }

            aiMsg.setContent(finalStoreText);
            messageMapper.insert(aiMsg);
        }
    }
}