package com.example.dairy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {
    static final UUID OK = UUID.fromString("00000000-0000-0000-0000-000000000100");
    @Autowired MockMvc mvc;

    @Test
    void anonymousCannotRead() throws Exception {
        mvc.perform(get("/api/segments")).andExpect(status().isUnauthorized());
    }

    @Test
    void operatorCannotReview() throws Exception {
        mvc.perform(post("/api/segments/{id}/review/lock", OK).with(httpBasic("operator","password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void labTechCannotIssueReview() throws Exception {
        mvc.perform(post("/api/segments/{id}/review/decision", OK)
                        .with(httpBasic("lab","password")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"notes\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewerCannotUpload() throws Exception {
        mvc.perform(multipart("/api/segments/{id}/imports", OK).file(
                new org.springframework.mock.web.MockMultipartFile("files","x.csv","text/csv","x".getBytes()))
                .with(httpBasic("reviewer","password")))
                .andExpect(status().isForbidden());
    }
}
