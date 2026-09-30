package com.dairy.homogenization.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void users() {
        jdbc.update("delete from evidence_snapshots; delete from reviews; delete from users where username in ('operator','analyst','reviewer')");
        jdbc.update("insert into users(id,username,full_name,role,password_hash,enabled) values (?,?,?,?,?,true)",
                101,"operator","操作员","OPERATOR","$2a$10$EuzFtC7RikZ6otB.0TK6pOmiNDswk./vCyM0AdGEo85heBE5xs.hC");
        jdbc.update("insert into users(id,username,full_name,role,password_hash,enabled) values (?,?,?,?,?,true)",
                102,"analyst","实验员","LAB_ANALYST","$2a$10$EuzFtC7RikZ6otB.0TK6pOmiNDswk./vCyM0AdGEo85heBE5xs.hC");
        jdbc.update("insert into users(id,username,full_name,role,password_hash,enabled) values (?,?,?,?,?,true)",
                103,"reviewer","审核员","REVIEWER","$2a$10$EuzFtC7RikZ6otB.0TK6pOmiNDswk./vCyM0AdGEo85heBE5xs.hC");
    }

    @Test
    void operatorCanReadButCannotApproveOrCorrectMapping() throws Exception {
        mvc.perform(get("/api/batches").with(httpBasic("operator","password"))).andExpect(status().isOk());
        mvc.perform(post("/api/reviews/comparisons/1/decision")
                .contentType("application/json").content("""
                {"approve":true,"decisionNote":"x"}""")
                .with(httpBasic("operator","password"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/pressure-mappings/correct")
                .contentType("application/json").content("""
                {"valveGroupId":1,"changeSummary":"x","items":[{"rawLabel":"P1","calibratedStageCode":"S1"}]}""")
                .with(httpBasic("operator","password"))).andExpect(status().isForbidden());
    }

    @Test
    void analystCanCorrectButCannotApprove() throws Exception {
        mvc.perform(post("/api/reviews/comparisons/1/decision")
                .contentType("application/json").content("""
                {"approve":true,"decisionNote":"x"}""")
                .with(httpBasic("analyst","password"))).andExpect(status().isForbidden());
    }

    @Test
    void reviewerCanReachDecisionEndpointButOperatorCannotUploadAsReviewer() throws Exception {
        mvc.perform(post("/api/reviews/comparisons/1/decision")
                .contentType("application/json").content("""
                {"approve":true,"decisionNote":"x"}""")
                .with(httpBasic("reviewer","password")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
    }
}
