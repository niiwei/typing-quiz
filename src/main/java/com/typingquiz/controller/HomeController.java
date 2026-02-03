package com.typingquiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 * 处理页面导航和根路径映射
 */
@Controller
public class HomeController {

    /**
     * 根路径映射 - 重定向到首页
     * GET /
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/home.html";
    }

    /**
     * 首页直接访问
     * GET /home
     */
    @GetMapping("/home")
    public String homePage() {
        return "forward:/home.html";
    }
}
