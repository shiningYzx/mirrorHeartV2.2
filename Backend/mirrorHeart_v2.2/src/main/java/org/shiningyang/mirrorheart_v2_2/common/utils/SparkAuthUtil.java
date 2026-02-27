package org.shiningyang.mirrorheart_v2_2.common.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 讯飞星火大模型 WebSocket 鉴权 URL 生成工具
 */
public class SparkAuthUtil {

    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        // 把 ws 替换成 http，wss 替换成 https，骗过 Java 解析器
        String parseUrl = hostUrl.replace("wss://", "https://").replace("ws://", "http://");
        URL url = new URL(parseUrl);

        // 时间格式化化为 RFC1123 格式，并设置 GMT 时区
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        // 拼接签名原始字符串
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";

        // HMAC-SHA256 加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        // 拼接 authorization 字符串
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);

        // Base64 编码 authorization
        String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        // 拼接最终的鉴权 URL
        return hostUrl + "?authorization=" + URLEncoder.encode(authBase, "UTF-8") +
                "&date=" + URLEncoder.encode(date, "UTF-8") +
                "&host=" + URLEncoder.encode(url.getHost(), "UTF-8");
    }
}