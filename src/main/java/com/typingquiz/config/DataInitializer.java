package com.typingquiz.config;

import com.typingquiz.entity.Answer;
import com.typingquiz.entity.Quiz;
import com.typingquiz.entity.FillBlankQuiz;
import com.typingquiz.entity.QuizType;
import com.typingquiz.repository.QuizRepository;
import com.typingquiz.repository.FillBlankQuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 在应用启动时创建示例测验数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final QuizRepository quizRepository;
    private final FillBlankQuizRepository fillBlankQuizRepository;

    @Autowired
    public DataInitializer(QuizRepository quizRepository, FillBlankQuizRepository fillBlankQuizRepository) {
        this.quizRepository = quizRepository;
        this.fillBlankQuizRepository = fillBlankQuizRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已有数据
        if (quizRepository.count() > 0) {
            System.out.println("数据库已有数据,跳过初始化");
            return;
        }

        System.out.println("开始初始化示例数据...");

        // 创建"世界首都"测验
        Quiz worldCapitals = new Quiz(
            "世界首都",
            "说出世界各国的首都城市",
            600  // 10分钟
        );

        // 添加答案
        String[] capitals = {
            "北京", "东京", "首尔", "平壤", "河内",
            "曼谷", "新加坡", "吉隆坡", "雅加达", "马尼拉",
            "新德里", "伊斯兰堡", "喀布尔", "德黑兰", "巴格达",
            "大马士革", "安卡拉", "莫斯科", "基辅", "华沙",
            "柏林", "巴黎", "伦敦", "罗马", "马德里",
            "里斯本", "阿姆斯特丹", "布鲁塞尔", "维也纳", "伯尔尼",
            "斯德哥尔摩", "奥斯陆", "哥本哈根", "赫尔辛基", "雅典",
            "开罗", "内罗毕", "约翰内斯堡", "拉各斯", "阿尔及尔",
            "华盛顿", "渥太华", "墨西哥城", "哈瓦那", "巴拿马城",
            "波哥大", "利马", "圣地亚哥", "布宜诺斯艾利斯", "巴西利亚",
            "堪培拉", "惠灵顿"
        };

        for (String capital : capitals) {
            Answer answer = new Answer(capital);
            worldCapitals.addAnswer(answer);
        }

        quizRepository.save(worldCapitals);

        System.out.println("创建测验: " + worldCapitals.getTitle());
        System.out.println("答案数量: " + worldCapitals.getAnswers().size());

        // 创建"古诗词填空"测验（填空题示例）
        Quiz poetryQuiz = new Quiz(
            "古诗词填空",
            "根据提示填写古诗词中的缺失部分",
            300  // 5分钟
        );
        poetryQuiz.setQuizType(QuizType.FILL_BLANK);
        Quiz savedPoetryQuiz = quizRepository.save(poetryQuiz);

        // 填空题信息（使用 ___ 占位符）
        String fullText = "床前明月光，疑是地上霜。举头望明月，低头思故乡。";
        String displayText = "床前___，疑是___。举头___，___思故乡。";
        String blanksInfo = "[{\"startIndex\":0,\"endIndex\":9,\"correctAnswer\":\"明月光\",\"comment\":\"床前\"},{\"startIndex\":18,\"endIndex\":27,\"correctAnswer\":\"地上霜\",\"comment\":\"疑是\"},{\"startIndex\":36,\"endIndex\":45,\"correctAnswer\":\"望明月\",\"comment\":\"举头\"},{\"startIndex\":54,\"endIndex\":63,\"correctAnswer\":\"低头\",\"comment\":\"思故乡\"}]";

        FillBlankQuiz fillBlankQuiz = new FillBlankQuiz(
            savedPoetryQuiz.getId(),
            fullText,
            blanksInfo,
            displayText,
            4
        );
        fillBlankQuizRepository.save(fillBlankQuiz);

        System.out.println("示例数据初始化完成!");
        System.out.println("创建测验: " + poetryQuiz.getTitle());
        System.out.println("填空题数量: 4");
    }
}
