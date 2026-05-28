package com.trustflow.cms_risk_service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.cms_risk_service.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessLogicIntegrationTest extends IntegrationTestBase {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRiskCategory_persistsCategoryForCompany() throws Exception {
        mockMvc.perform(post("/api/risk-categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Fraud"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fraud"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void listRiskCategories_returnsCreatedCategory() throws Exception {
        String categoryName = "Payments-" + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/risk-categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(categoryName)))
                .andExpect(status().isCreated())
                .andReturn();

        String categoryId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/risk-categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + categoryId + "')].name").value(categoryName));
    }

    @Test
    void createRiskCategory_returnsConflictForDuplicateName() throws Exception {
        String body = """
                {"name":"Duplicate"}
                """;
        mockMvc.perform(post("/api/risk-categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/risk-categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Категория с таким названием уже существует"));
    }

    @Test
    void createRule_persistsRuleLinkedToCategory() throws Exception {
        UUID categoryId = createCategory("Operational");

        MvcResult createRuleResult = mockMvc.perform(post("/api/rules")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Large transfer",
                                  "condition": "amount > 10000",
                                  "categoryId": "%s",
                                  "priority": "high",
                                  "responsibleUserId": "%s",
                                  "actions": ["createIncident"],
                                  "enabled": true
                                }
                                """.formatted(categoryId, TEST_USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(createRuleResult.getResponse().getContentAsString());
        assertThat(response.get("id").asText()).isNotBlank();
    }

    @Test
    void getRuleDetails_returnsPersistedRule() throws Exception {
        UUID categoryId = createCategory("Compliance");
        UUID ruleId = createRule(categoryId, "AML check", "client.riskScore > 80");

        mockMvc.perform(get("/api/rules/{id}", ruleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ruleId.toString()))
                .andExpect(jsonPath("$.name").value("AML check"))
                .andExpect(jsonPath("$.condition").value("client.riskScore > 80"));
    }

    @Test
    void updateRule_changesRuleFields() throws Exception {
        UUID categoryId = createCategory("Credit");
        UUID ruleId = createRule(categoryId, "Initial rule", "amount > 1000");

        mockMvc.perform(put("/api/rules/{id}", ruleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Threshold increased",
                                  "name": "Updated rule",
                                  "condition": "amount > 5000",
                                  "categoryId": "%s",
                                  "priority": "medium",
                                  "responsibleUserId": "%s",
                                  "actions": ["sendNotification"],
                                  "enabled": false
                                }
                                """.formatted(categoryId, TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ruleId.toString()));

        mockMvc.perform(get("/api/rules/{id}", ruleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated rule"))
                .andExpect(jsonPath("$.condition").value("amount > 5000"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    private UUID createCategory(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/risk-categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createRule(UUID categoryId, String name, String condition) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rules")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken())
                        .header("CompanyId", TEST_COMPANY_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "condition": "%s",
                                  "categoryId": "%s",
                                  "priority": "low",
                                  "actions": ["createIncident"],
                                  "enabled": true
                                }
                                """.formatted(name, condition, categoryId)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
