package org.shiningyang.mirrorheart_v2_2.module.todo.task;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.utils.SparkAuthUtil;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.shiningyang.mirrorheart_v2_2.module.todo.entity.TodoItem;
import org.shiningyang.mirrorheart_v2_2.module.todo.service.ITodoItemService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoWeeklySummaryTask {

    private final ITodoItemService todoItemService;
    private final INotificationService notificationService;

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

    /**
     * 每周日晚上 20:00 自动执行 (Cron 表达式)
     */
    @Scheduled(cron = "0 0 20 ? * SUN")
    public void executeWeeklySummary() {
        log.info("== 开始生成每周待办微光 AI 总结 ==");

        // 1. 计算本周的起止时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).withHour(23).withMinute(59).withSecond(59);

        // 2. 聚合查询：找出本周完成过待办的用户及完成数量
        QueryWrapper<TodoItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id", "count(id) as count")
                .eq("status", 1)
                .ge("completed_at", startOfWeek)
                .le("completed_at", endOfWeek)
                .groupBy("user_id");

        List<Map<String, Object>> userStats = todoItemService.listMaps(queryWrapper);

        if (userStats == null || userStats.isEmpty()) {
            log.info("本周没有用户点亮微光，跳过总结。");
            return;
        }

        int successCount = 0;
        for (Map<String, Object> stat : userStats) {
            // 注意：不同数据库驱动返回的列名大小写可能不同，通过 Number 强转是最安全的
            Long userId = ((Number) stat.getOrDefault("user_id", stat.get("USER_ID"))).longValue();
            int count = ((Number) stat.getOrDefault("count", stat.get("COUNT"))).intValue();

            String aiMessage;
            try {
                // 3. 调用 AI 生成专属总结
                aiMessage = generateSummarySync(count);
                // 稍微休眠，避免并发过高触发大模型 QPS 限制
                Thread.sleep(800);
            } catch (Exception e) {
                log.error("为用户 {} 生成每周AI总结失败，已降级为后备默认文案", userId, e);
                // 【核心兜底】：哪怕 AI 断网了，用户依然能收到鼓励！
                aiMessage = String.format("这周你亲手点亮了 %d 颗微光，你的星空已经初具规模了。生活是由一个个微小的成就组成的，感谢这周对自己如此用心的你。下周也请继续发光吧！", count);
            }

            // 4. 作为系统通知发送给用户 (senderId = 1 代表系统管理员发送)
            notificationService.createNotification(userId, 1L, "SYSTEM", "TODO_SUMMARY", null, aiMessage);
            successCount++;
        }
        log.info("== 每周待办总结生成完毕，共发送 {} 条星空通知 ==", successCount);
    }

    /**
     * 内部方法：同步阻塞调用星火大模型获取总结文本
     */
    private String generateSummarySync(int count) throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // 精心设计的 Prompt 提示词
        String prompt = String.format("我这周完成了 %d 个微光（生活中的小目标）。请作为一位温柔的心理教练，用一段60字左右的文字总结并鼓励我。要求：第一句话必须严格是：“这周你亲手点亮了 %d 颗微光，你的星空已经初具规模了。” 后面接着正能量的治愈话语，不要有多余的解释，不要带换行符。", count, count);

        String requestJson = buildSparkRequest(prompt);
        String authUrl = SparkAuthUtil.getAuthUrl(hostUrl, apiKey, apiSecret);

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(authUrl), new SparkSyncWebSocketListener(future))
                .thenAccept(ws -> ws.sendText(requestJson, true));

        // 最多等待 15 秒，超时则抛出异常进入后备方案
        return future.get(15, TimeUnit.SECONDS);
    }

    private String buildSparkRequest(String content) {
        JSONObject request = new JSONObject();
        JSONObject header = new JSONObject().set("app_id", appId).set("uid", "weekly_summary");
        request.set("header", header);
        
        // 配置模型参数，降低 max_tokens 提升响应速度
        JSONObject parameter = new JSONObject().set("chat", new JSONObject().set("domain", domain).set("max_tokens", 150));
        request.set("parameter", parameter);
        
        JSONObject payload = new JSONObject();
        JSONArray textArray = new JSONArray();
        textArray.add(new JSONObject().set("role", "user").set("content", content));
        payload.set("message", new JSONObject().set("text", textArray));
        request.set("payload", payload);
        
        return request.toString();
    }

    /**
     * 同步流收集器
     */
    static class SparkSyncWebSocketListener implements WebSocket.Listener {
        private final CompletableFuture<String> future;
        private final StringBuilder fullContent = new StringBuilder();

        public SparkSyncWebSocketListener(CompletableFuture<String> future) {
            this.future = future;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                JSONObject response = JSONUtil.parseObj(data.toString());
                int code = response.getJSONObject("header").getInt("code");
                if (code != 0) {
                    future.completeExceptionally(new RuntimeException("星火接口返回错误: " + response.getJSONObject("header").getStr("message")));
                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "error");
                    return null;
                }
                JSONObject payload = response.getJSONObject("payload");
                if (payload != null && payload.containsKey("choices")) {
                    JSONObject choices = payload.getJSONObject("choices");
                    JSONArray textArray = choices.getJSONArray("text");
                    if (textArray != null && !textArray.isEmpty()) {
                        // 过滤掉深度思考 <think> 的内容，只要最终回复
                        String content = textArray.getJSONObject(0).getStr("content");
                        if (content != null) {
                            fullContent.append(content);
                        }
                    }
                    if (choices.getInt("status") == 2) {
                        future.complete(fullContent.toString());
                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
                    }
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            future.completeExceptionally(error);
        }
    }
}