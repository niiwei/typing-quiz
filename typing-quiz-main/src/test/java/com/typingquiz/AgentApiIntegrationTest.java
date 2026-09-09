package com.typingquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingquiz.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void patIsOneTimeAndAgentApiIsPatOnly() throws Exception {
        String jwt = JwtUtil.generateToken(101L, "agent-user");
        String patResponse = mockMvc.perform(post("/api/personal-access-tokens")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Codex\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode pat = objectMapper.readTree(patResponse);
        String patValue = pat.get("token").asText();
        assertThat(patValue).startsWith("mp_pat_");
        String listed = mockMvc.perform(get("/api/personal-access-tokens").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(listed).get(0).get("token").isNull()).isTrue();

        mockMvc.perform(get("/api/agent/v1/quizzes").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/agent/v1/quizzes").header("Authorization", "Bearer " + patValue))
                .andExpect(status().isOk());

        String created = mockMvc.perform(post("/api/agent/v1/quizzes")
                        .header("Authorization", "Bearer " + patValue)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Agent quiz\",\"quizType\":\"TYPING\",\"answerList\":[{\"content\":\"answer\"}],\"groups\":[\"Work\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode createdQuiz = objectMapper.readTree(created);
        assertThat(createdQuiz.get("groups").toString()).contains("Work");
        long quizId = createdQuiz.get("id").asLong();
        long version = createdQuiz.get("version").asLong();
        String updated = mockMvc.perform(patch("/api/agent/v1/quizzes/{id}", quizId)
                        .header("Authorization", "Bearer " + patValue).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version + ",\"description\":\"changed\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long nextVersion = objectMapper.readTree(updated).get("version").asLong();
        assertThat(nextVersion).isGreaterThan(version);
        mockMvc.perform(patch("/api/agent/v1/quizzes/{id}", quizId)
                        .header("Authorization", "Bearer " + patValue).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version + ",\"title\":\"stale\"}"))
                .andExpect(status().isConflict());
        JsonNode preview = objectMapper.readTree(mockMvc.perform(post("/api/agent/v1/quizzes/{id}/delete-preview", quizId)
                        .header("Authorization", "Bearer " + patValue)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mockMvc.perform(delete("/api/agent/v1/quizzes/{id}", quizId).header("Authorization", "Bearer " + patValue)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmationToken\":\"" + preview.get("confirmationToken").asText() + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/personal-access-tokens/{id}", pat.get("id").asLong())
                        .header("Authorization", "Bearer " + jwt)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/agent/v1/quizzes").header("Authorization", "Bearer " + patValue))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importIsAtomicAndIdempotent() throws Exception {
        String jwt = JwtUtil.generateToken(102L, "import-user");
        String pat = objectMapper.readTree(mockMvc.perform(post("/api/personal-access-tokens")
                        .header("Authorization", "Bearer " + jwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"import\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("token").asText();
        String body = "{\"requestId\":\"550e8400-e29b-41d4-a716-446655440000\",\"quizzes\":[{\"title\":\"one\",\"quizType\":\"TYPING\",\"answerList\":[{\"content\":\"ok\"}]},{\"title\":\"bad\",\"quizType\":\"FILL_BLANK\"}]}";
        mockMvc.perform(post("/api/agent/v1/imports").header("Authorization", "Bearer " + pat)
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnprocessableEntity());
        JsonNode list = objectMapper.readTree(mockMvc.perform(get("/api/agent/v1/quizzes").header("Authorization", "Bearer " + pat))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(list.get("total").asInt()).isZero();

        String valid = "{\"requestId\":\"550e8400-e29b-41d4-a716-446655440001\",\"quizzes\":[{\"title\":\"one\",\"quizType\":\"TYPING\",\"answerList\":[{\"content\":\"ok\"}]}]}";
        String first = mockMvc.perform(post("/api/agent/v1/imports").header("Authorization", "Bearer " + pat)
                        .contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String replay = mockMvc.perform(post("/api/agent/v1/imports").header("Authorization", "Bearer " + pat)
                        .contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(replay).isEqualTo(first);
    }

    @Test
    void deletingGroupOnlyRemovesMembership() throws Exception {
        String ownerJwt = JwtUtil.generateToken(103L, "group-owner");
        String pat = objectMapper.readTree(mockMvc.perform(post("/api/personal-access-tokens")
                        .header("Authorization", "Bearer " + ownerJwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"group\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("token").asText();
        JsonNode quiz = objectMapper.readTree(mockMvc.perform(post("/api/agent/v1/quizzes")
                        .header("Authorization", "Bearer " + pat).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"kept\",\"quizType\":\"TYPING\",\"answerList\":[{\"content\":\"answer\"}],\"groups\":[\"Keep\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long quizId = quiz.get("id").asLong();
        JsonNode groups = objectMapper.readTree(mockMvc.perform(get("/api/agent/v1/groups").header("Authorization", "Bearer " + pat))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode keep = null;
        for (JsonNode item : groups) if ("Keep".equals(item.get("name").asText())) keep = item;
        assertThat(keep).isNotNull();
        JsonNode preview = objectMapper.readTree(mockMvc.perform(post("/api/agent/v1/groups/{id}/delete-preview", keep.get("id").asLong())
                .header("Authorization", "Bearer " + pat)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mockMvc.perform(delete("/api/agent/v1/groups/{id}", keep.get("id").asLong()).header("Authorization", "Bearer " + pat)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmationToken\":\"" + preview.get("confirmationToken").asText() + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/agent/v1/quizzes/{id}", quizId).header("Authorization", "Bearer " + pat)).andExpect(status().isOk());
        mockMvc.perform(get("/api/agent/v1/groups/{id}", keep.get("id").asLong()).header("Authorization", "Bearer " + pat)).andExpect(status().isNotFound());

        String otherPat = objectMapper.readTree(mockMvc.perform(post("/api/personal-access-tokens")
                        .header("Authorization", "Bearer " + JwtUtil.generateToken(104L, "other"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"other\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(get("/api/agent/v1/quizzes/{id}", quizId).header("Authorization", "Bearer " + otherPat)).andExpect(status().isNotFound());
    }
}
