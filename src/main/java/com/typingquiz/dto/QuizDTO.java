package com.typingquiz.dto;

import com.typingquiz.entity.QuizType;
import java.util.List;

/**
 * 测验数据传输对象
 * 用于创建测验的请求
 */
public class QuizDTO {

    private String title;
    private String description;
    private Integer timeLimit;
    private List<String> answers;
    private List<AnswerCreateDTO> answerList;
    private QuizType quizType;
    private FillBlankQuizDTO fillBlankQuiz;

    public QuizDTO() {
    }

    public QuizDTO(String title, String description, Integer timeLimit, List<String> answers) {
        this.title = title;
        this.description = description;
        this.timeLimit = timeLimit;
        this.answers = answers;
        this.quizType = QuizType.TYPING;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public List<AnswerCreateDTO> getAnswerList() {
        return answerList;
    }

    public void setAnswerList(List<AnswerCreateDTO> answerList) {
        this.answerList = answerList;
    }

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
    }

    public FillBlankQuizDTO getFillBlankQuiz() {
        return fillBlankQuiz;
    }

    public void setFillBlankQuiz(FillBlankQuizDTO fillBlankQuiz) {
        this.fillBlankQuiz = fillBlankQuiz;
    }

    /**
     * 获取包含注释的答案列表（用于导出）
     */
    public List<AnswerCreateDTO> getAnswersWithComments() {
        if (answerList != null && !answerList.isEmpty()) {
            return answerList;
        }
        return null;
    }
}
