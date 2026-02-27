package org.shiningyang.mirrorheart_v2_2;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * MyBatis-Plus 代码生成器 (适配 org.shiningyang 包路径)
 */
public class CodeGenerator {

    // TODO: 记得确认数据库密码
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mirrot_heart_v2_2?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Yzx030927";

    public static void main(String[] args) {
        // 定义模块与表的对应关系
        Map<String, String[]> modules = new HashMap<>();

        modules.put("auth", new String[]{"user", "auth_session", "email_otp"});
        modules.put("admin", new String[]{"admin_role", "admin_permission", "admin_user_role", "admin_role_permission", "admin_audit_log"});
        modules.put("content", new String[]{"content", "content_quote", "content_article", "content_book", "content_movie", "content_music", "content_painting", "content_tag_relation", "tag"});
        modules.put("recommend", new String[]{"daily_recommendation", "daily_recommendation_item"});
        modules.put("community", new String[]{"post", "post_image", "post_audio", "post_tag_relation", "comment"});
        modules.put("interaction", new String[]{"like_action", "favorite_action", "user_relation"});
        modules.put("question", new String[]{"daily_question", "user_daily_record"});
        modules.put("ai", new String[]{"ai_chat_session", "ai_chat_message"});
        modules.put("todo", new String[]{"todo_item", "todo_recommendation"});
        modules.put("system", new String[]{"notification", "sys_file", "sys_config", "app_version", "search_history", "sys_sensitive_word", "report"});

        modules.forEach(CodeGenerator::generateModule);

        System.out.println("\n🎉 所有模块代码生成完成！");
    }

    private static void generateModule(String moduleName, String[] tables) {
        String projectPath = System.getProperty("user.dir");

        FastAutoGenerator.create(DB_URL, DB_USER, DB_PASSWORD)
                .globalConfig(builder -> {
                    builder.author("ShiningYang")
                            .outputDir(projectPath + "/src/main/java")
                            .disableOpenDir();
                })
                .packageConfig(builder -> {
                    // 核心修复点：修改父包名为你的项目实际包名
                    builder.parent("org.shiningyang.mirrorheart_v2_2.module")
                            .moduleName(moduleName)
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper/" + moduleName));
                })
                .strategyConfig(builder -> {
                    builder.addInclude(tables)
                            .addTablePrefix("sys_")
                            .entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .logicDeleteColumnName("is_deleted")
                            .controllerBuilder()
                            .enableRestStyle();
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("✅ 模块 [" + moduleName + "] 生成成功");
    }
}