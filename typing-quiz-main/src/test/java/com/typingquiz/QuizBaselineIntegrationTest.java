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
}
