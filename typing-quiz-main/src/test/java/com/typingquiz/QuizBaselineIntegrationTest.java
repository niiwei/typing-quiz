package com.typingquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.typingquiz.util.JwtUtil;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuizBaselineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsReadsAndValidatesLegacyTypingQuizWithComment() throws Exception {
        String request = "{" +
                "\"title\":\"Baseline typing quiz\"," +
                "\"description\":\"isolated baseline\"," +
                "\"quizType\":\"TYPING\"," +
                "\"answerList\":[" +
                "{\"content\":\"robots.txt\",\"comment\":\"网站抓取规则\"}," +
                "{\"content\":\"llms.txt\",\"comment\":\"AI 文档索引\"}" +
                "],\"groups\":[]}";
        String token = JwtUtil.generateToken(7L, "baseline-user");

        String created = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode createdJson = objectMapper.readTree(created);
        long quizId = createdJson.get("id").asLong();

        String detail = mockMvc.perform(get("/api/quizzes/{id}", quizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode detailJson = objectMapper.readTree(detail);
        assertThat(detailJson.get("answerList")).hasSize(2);
        assertThat(detailJson.get("answerList").get(0).get("comment").asText())
                .isEqualTo("网站抓取规则");

        String validation = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId + ",\"input\":\"robots.txt\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode validationJson = objectMapper.readTree(validation);
        assertThat(validationJson.get("valid").asBoolean()).isTrue();
        assertThat(validationJson.get("displayContent").asText()).isEqualTo("robots.txt");
    }

    @Test
    void appliesTheSameWhitespaceAndCaseSettingsToBackendFallback() throws Exception {
        String token = JwtUtil.generateToken(8L, "settings-user");
        String request = "{\"title\":\"Normalization quiz\",\"quizType\":\"TYPING\"," +
                "\"answerList\":[{\"content\":\"Token数\",\"comment\":\"metadata\"}],\"groups\":[]}";
        String created = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long quizId = objectMapper.readTree(created).get("id").asLong();

        String enabled = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId +
                                ",\"input\":\"Token　数\",\"ignoreSpaces\":true," +
                                "\"ignoreCase\":true,\"ignorePunctuation\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(enabled).get("valid").asBoolean()).isTrue();

        String disabled = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId +
                                ",\"input\":\"Token　数\",\"ignoreSpaces\":false," +
                                "\"ignoreCase\":true,\"ignorePunctuation\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(disabled).get("valid").asBoolean()).isFalse();

        String edgeSpacesEnabled = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId +
                                ",\"input\":\" Token数 \",\"ignoreSpaces\":true," +
                                "\"ignoreCase\":true," +
                                "\"ignorePunctuation\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(edgeSpacesEnabled).get("valid").asBoolean()).isTrue();

        String edgeSpacesDisabled = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId +
                                ",\"input\":\" Token数 \",\"ignoreSpaces\":false," +
                                "\"ignoreCase\":true," +
                                "\"ignorePunctuation\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(edgeSpacesDisabled).get("valid").asBoolean()).isFalse();
    }

    @Test
    void doesNotTreatEmptyOrPunctuationOnlyInputAsAnAnswer() throws Exception {
        String token = JwtUtil.generateToken(9L, "empty-user");
        String request = "{\"title\":\"Symbols quiz\",\"quizType\":\"TYPING\"," +
                "\"answerList\":[{\"content\":\"C++\"}],\"groups\":[]}";
        String created = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long quizId = objectMapper.readTree(created).get("id").asLong();

        String empty = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId + ",\"input\":\"   \"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(empty).get("valid").asBoolean()).isFalse();

        String symbols = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId + ",\"input\":\"+++\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(symbols).get("valid").asBoolean()).isFalse();

        String punctuationOnlyAnswer = "{\"title\":\"Punctuation answer\",\"quizType\":\"TYPING\"," +
                "\"answerList\":[{\"content\":\"!!!\"}],\"groups\":[]}";
        String punctuationQuiz = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(punctuationOnlyAnswer))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long punctuationQuizId = objectMapper.readTree(punctuationQuiz).get("id").asLong();
        String punctuationOnlyInput = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + punctuationQuizId + ",\"input\":\"???\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(punctuationOnlyInput).get("valid").asBoolean()).isFalse();
    }

    @Test
    void persistsV2ContextAndAcceptsRequiredKeywordOrFullSentence() throws Exception {
        String token = JwtUtil.generateToken(10L, "v2-user");
        String request = "{\"title\":\"Context quiz\",\"quizType\":\"TYPING\",\"answerList\":[" +
                "{\"content\":\"发布 llms.txt 文档索引\",\"comment\":\"AI 文档入口\"," +
                "\"formatVersion\":2,\"parts\":[{\"segments\":[" +
                "{\"kind\":\"context\",\"text\":\"发布 \"}," +
                "{\"kind\":\"required\",\"text\":\"llms.txt\"}," +
                "{\"kind\":\"context\",\"text\":\" 文档索引\"}]}]}],\"groups\":[]}";
        String created = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long quizId = objectMapper.readTree(created).get("id").asLong();

        String detail = mockMvc.perform(get("/api/quizzes/{id}", quizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode answer = objectMapper.readTree(detail).get("answerList").get(0);
        assertThat(answer.get("formatVersion").asInt()).isEqualTo(2);
        assertThat(answer.get("parts").get(0).get("segments")).hasSize(3);

        for (String input : new String[]{"llms.txt", "发布 llms.txt 文档索引"}) {
            String validation = mockMvc.perform(post("/api/answers/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quizId\":" + quizId + ",\"input\":\"" + input + "\"}"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(objectMapper.readTree(validation).get("valid").asBoolean()).isTrue();
        }
    }

    @Test
    void returnsEveryMatchingAnswerAndPartForSharedKeyword() throws Exception {
        String token = JwtUtil.generateToken(11L, "parts-user");
        String request = "{\"title\":\"Parts quiz\",\"quizType\":\"TYPING\",\"answerList\":[" +
                "{\"content\":\"降低成本、提高效率\",\"formatVersion\":2,\"parts\":[" +
                "{\"segments\":[{\"kind\":\"required\",\"text\":\"降低成本\"},{\"kind\":\"context\",\"text\":\"、\"}]}," +
                "{\"segments\":[{\"kind\":\"required\",\"text\":\"提高效率\"}]}]}," +
                "{\"content\":\"提高效率\",\"formatVersion\":2,\"parts\":[" +
                "{\"segments\":[{\"kind\":\"required\",\"text\":\"提高效率\"}]}]}," +
                "{\"content\":\"提高效率\",\"comment\":\"重复要点仍保留\",\"formatVersion\":2,\"parts\":[" +
                "{\"segments\":[{\"kind\":\"required\",\"text\":\"提高效率\"}]}]}],\"groups\":[]}";
        String created = mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long quizId = objectMapper.readTree(created).get("id").asLong();

        String validation = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId + ",\"input\":\"提高效率\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode matches = objectMapper.readTree(validation).get("matches");
        assertThat(matches).hasSize(3);
        assertThat(matches.get(0).get("partIndices").get(0).asInt()).isEqualTo(1);
        assertThat(matches.get(1).get("partIndices").get(0).asInt()).isEqualTo(0);
    }

    /** 验证 skill v2 样例的导入、作答、导出和再次导入链路。 */
    @Test
    void importsAnswersFromSkillSampleAnswersExportsAndReimports() throws Exception {
        String token = JwtUtil.generateToken(12L, "skill-sample-user");
        String sample = Files.readString(Path.of(".scratch/answer-keypoints/skill-valid-sample.json"));

        String imported = mockMvc.perform(post("/api/import-export/quizzes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(sample))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode importResult = objectMapper.readTree(imported);
        assertThat(importResult.get("successCount").asInt()).isEqualTo(1);
        assertThat(importResult.get("failureCount").asInt()).isZero();
        long quizId = importResult.get("successes").get(0).get("id").asLong();

        for (String input : new String[]{
                "llms.txt", "提高效率", "降低成本", "降低成本、提高效率", "降低成本提高效率"}) {
            String validation = mockMvc.perform(post("/api/answers/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quizId\":" + quizId + ",\"input\":\"" + input + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(objectMapper.readTree(validation).get("valid").asBoolean()).isTrue();
        }
        String requiredOnlyWithoutPunctuation = mockMvc.perform(post("/api/answers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quizId +
                                ",\"input\":\"降低成本提高效率\",\"ignorePunctuation\":false," +
                                "\"ignoreSpaces\":true,\"ignoreCase\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode requiredOnlyResult = objectMapper.readTree(requiredOnlyWithoutPunctuation);
        assertThat(requiredOnlyResult.get("valid").asBoolean()).isTrue();
        assertThat(requiredOnlyResult.get("matches").findValue("partIndices")).hasSize(2);

        String exported = mockMvc.perform(get("/api/import-export/quiz/{id}/export", quizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode exportedQuiz = objectMapper.readTree(exported);
        assertThat(exportedQuiz.get("answerList")).hasSize(8);
        assertThat(exportedQuiz.get("answerList").get(6).get("parts")).hasSize(2);

        String reimported = mockMvc.perform(post("/api/import-export/quiz/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(exported))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(reimported).contains("测验导入成功");
        long reimportedQuizId = Long.parseLong(reimported.substring(reimported.lastIndexOf(':') + 1).trim());
        String reimportedDetail = mockMvc.perform(get("/api/quizzes/{id}", reimportedQuizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(reimportedDetail).get("answerList").get(6).get("parts"))
                .hasSize(2);

        String invalidSample = Files.readString(Path.of(".scratch/answer-keypoints/skill-invalid-sample.json"));
        String rejected = mockMvc.perform(post("/api/import-export/quizzes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(invalidSample))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode rejectionResult = objectMapper.readTree(rejected);
        assertThat(rejectionResult.get("successCount").asInt()).isZero();
        assertThat(rejectionResult.get("failureCount").asInt()).isEqualTo(1);
    }
}
