package com.typingquiz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.dto.QuizDTO;
import com.typingquiz.entity.User;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.repository.UserRepository;
import com.typingquiz.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(QuizRepository quizRepository, QuizService quizService, ObjectMapper objectMapper, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.quizRepository = quizRepository;
        this.quizService = quizService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (quizRepository.count() > 0) {
            System.out.println("数据库已有数据,跳过初始化");
            return;
        }

        System.out.println("开始初始化示例数据...");

        // Create a template user for initial quizzes
        User templateUser = createTemplateUser();

        loadQuizFromClasspath("initial-data/initial-capitals.json", templateUser.getId());
        loadQuizFromClasspath("initial-data/initial-poetry.json", templateUser.getId());

        System.out.println("示例数据初始化完成!");
    }

    private User createTemplateUser() {
        String templateUsername = "template_user";
        Optional<User> existingUser = userRepository.findByUsername(templateUsername);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        User user = new User();
        user.setUsername(templateUsername);
        user.setEmail(templateUsername + "@example.com");
        user.setPassword(passwordEncoder.encode("a_very_secure_password_placeholder"));
        return userRepository.save(user);
    }

    private void loadQuizFromClasspath(String path, Long userId) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream inputStream = resource.getInputStream()) {
                QuizDTO quizDTO = objectMapper.readValue(inputStream, QuizDTO.class);
                quizService.createQuiz(quizDTO, userId);
                System.out.println("成功加载并创建测验: " + quizDTO.getTitle());
            }
        } catch (Exception e) {
            System.err.println("加载初始化数据失败: " + path);
            e.printStackTrace();
        }
    }
}
