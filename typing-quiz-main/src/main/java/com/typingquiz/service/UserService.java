package com.typingquiz.service;

import com.typingquiz.dto.QuizDTO;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.User;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, QuizRepository quizRepository, QuizService quizService) {
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.quizService = quizService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }
        User newUser = new User(username, email, passwordEncoder.encode(password));
        User savedUser = userRepository.save(newUser);

        // Copy template quizzes to the new user
        copyTemplateQuizzesToUser(savedUser);

        return savedUser;
    }

    private void copyTemplateQuizzesToUser(User newUser) {
        userRepository.findByUsername("template_user").ifPresent(templateUser -> {
            List<Quiz> templateQuizzes = quizRepository.findByUserId(templateUser.getId());
            for (Quiz templateQuiz : templateQuizzes) {
                QuizDTO quizDTO = quizService.convertToDTO(templateQuiz);
                quizService.createQuiz(quizDTO, newUser.getId());
            }
        });
    }

    public Optional<User> login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return user;
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
