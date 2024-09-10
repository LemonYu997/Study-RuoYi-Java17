package com.lemon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 */
@SpringBootApplication
public class LemonApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LemonApplication.class);
        // 启动方式设置为缓冲，可以记录启动日志
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Lemon-Vue-Plus启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
