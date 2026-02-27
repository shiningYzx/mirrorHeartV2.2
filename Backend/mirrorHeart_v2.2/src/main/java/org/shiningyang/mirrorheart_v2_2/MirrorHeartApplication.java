package org.shiningyang.mirrorheart_v2_2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // [新增] 开启定时任务支持
@EnableAsync // [新增] 开启异步任务支持，用于给粉丝异步发通知
@MapperScan("org.shiningyang.mirrorheart_v2_2.module.**.mapper")
public class MirrorHeartApplication {

    public static void main(String[] args) {
        SpringApplication.run(MirrorHeartApplication.class, args);
    }

}
