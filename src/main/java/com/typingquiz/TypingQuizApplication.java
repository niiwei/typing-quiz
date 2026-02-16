package com.typingquiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * JetPunk风格打字测验应用主类
 */
@SpringBootApplication
public class TypingQuizApplication {

    @javax.annotation.PostConstruct
    public void init() {
        // 强制全局使用北京时间，防止 8 小时时区偏移导致的复习逻辑失效
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(TypingQuizApplication.class, args);
    }
}
