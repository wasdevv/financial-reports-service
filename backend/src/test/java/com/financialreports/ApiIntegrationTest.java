package com.financialreports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A API inteira contra Postgres real, com o schema criado pela mesma migration Flyway de produção.
 * Login de verdade (bcrypt + JWT assinado); nenhum service ou repositório mockado.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-that-is-long-enough-for-hs256-signing",
        "app.admin.email=admin@test.io",
        "app.admin.password=admin-password-123",
        "app.cors-origins=https://frontend.test"})
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    String admin;
    String alice;
    String bob;

    @BeforeEach
    void users() throws Exception {
        admin = login("admin@test.io", "admin-password-123");
        alice = register("alice");
        bob = register("bob");
    }

    // ---------- helpers ----------

    String uniq(String name) {
        return name + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    String register(String name) throws Exception {
        String u = uniq(name);
        return body(call(post("/api/v1/auth/register"), null,
                "{\"username\":\"%s\",\"email\":\"%s@x.io\",\"fullName\":\"%s\",\"password\":\"password123\"}"
                        .formatted(u, u, name), status().isCreated())).get("token").asText();
    }

    String login(String email, String password) throws Exception {
        return body(call(post("/api/v1/auth/login"), null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password), status().isOk()))
                .get("token").asText();
    }

    MvcResult call(MockHttpServletRequestBuilder req, String token, String body, ResultMatcher expected)
            throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(req).andExpect(expected).andReturn();
    }

    JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    long createReport(String token, String title) throws Exception {
        return body(call(post("/api/v1/reports"), token,
                "{\"title\":\"%s\",\"period\":\"2026-03\"}".formatted(title), status().isCreated()))
                .get("id").asLong();
    }

    JsonNode addRecord(String token, long id, String type, String category, String amount) throws Exception {
        return body(call(post("/api/v1/reports/" + id + "/records"), token,
                "{\"recordDate\":\"2026-03-10\",\"type\":\"%s\",\"category\":\"%s\",\"amount\":%s}"
                        .formatted(type, category, amount), status().isCreated()));
    }

    long userId(String username) throws Exception {
        for (JsonNode u : body(call(get("/api/v1/admin/users"), admin, null, status().isOk()))) {
            if (u.get("username").asText().equals(username)) return u.get("id").asLong();
        }
        throw new AssertionError("no user " + username);
    }

    String me(String token) throws Exception {
        return body(call(get("/api/v1/auth/me"), token, null, status().isOk())).get("username").asText();
    }

    // ---------- auth ----------

    @Test
    void registerIgnoresRoleInPayloadAndRejectsDuplicates() throws Exception {
        String u = uniq("mallory");
        String payload = "{\"username\":\"%s\",\"email\":\"%s@x.io\",\"fullName\":\"M\",\"password\":\"password123\",\"role\":\"ADMIN\"}"
                .formatted(u, u);
        JsonNode created = body(call(post("/api/v1/auth/register"), null, payload, status().isCreated()));
        assertThat(created.at("/user/role").asText()).isEqualTo("ANALYST");
        assertThat(created.get("type").asText()).isEqualTo("Bearer");

        String sameEmailOtherCase = "{\"username\":\"%s\",\"email\":\"%s@X.IO\",\"fullName\":\"M\",\"password\":\"password123\"}"
                .formatted(uniq("other"), u.toUpperCase());
        call(post("/api/v1/auth/register"), null, sameEmailOtherCase, status().isConflict());
    }

    @Test
    void registerValidatesFields() throws Exception {
        JsonNode err = body(call(post("/api/v1/auth/register"), null,
                "{\"username\":\"a b\",\"email\":\"nope\",\"fullName\":\"\",\"password\":\"short\"}",
                status().isBadRequest()));
        assertThat(err.get("errors").fieldNames()).toIterable()
                .contains("username", "email", "fullName", "password");
    }

    @Test
    void badCredentialsAndBadTokensAre401() throws Exception {
        call(post("/api/v1/auth/login"), null, "{\"email\":\"admin@test.io\",\"password\":\"wrong-password\"}",
                status().isUnauthorized());
        call(post("/api/v1/auth/login"), null, "{\"email\":\"ghost@test.io\",\"password\":\"whatever1\"}",
                status().isUnauthorized());
        call(get("/api/v1/reports"), null, null, status().isUnauthorized());
        call(get("/api/v1/reports"), "not.a.jwt", null, status().isUnauthorized());
        String tampered = alice.substring(0, alice.length() - 2) + (alice.endsWith("AA") ? "BB" : "AA");
        call(get("/api/v1/auth/me"), tampered, null, status().isUnauthorized());
    }

    @Test
    void disablingAUserKillsTheirExistingToken() throws Exception {
        long id = userId(me(bob));
        call(patch("/api/v1/admin/users/" + id), admin, "{\"active\":false}", status().isOk());
        call(get("/api/v1/auth/me"), bob, null, status().isUnauthorized());
        call(patch("/api/v1/admin/users/" + id), admin, "{\"active\":true}", status().isOk());
        call(get("/api/v1/auth/me"), bob, null, status().isOk());
    }

    // ---------- workflow ----------

    @Test
    void fullReportWorkflowWithRiskAndAudit() throws Exception {
        long id = createReport(alice, "Q1 store");

        addRecord(alice, id, "INCOME", "Sales", "1000.00");
        JsonNode detail = addRecord(alice, id, "EXPENSE", "Fuel", "1500.50");
        assertThat(detail.get("totalExpense").decimalValue()).isEqualByComparingTo("1500.50");
        assertThat(detail.get("riskScore").asInt()).isEqualTo(55); // DEFICIT 40 + CONCENTRATION 15
        assertThat(detail.get("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(detail.get("riskFactors")).hasSize(2);

        // removendo o gasto, score e totais são recalculados
        long expenseId = detail.get("records").get(1).get("id").asLong();
        detail = body(call(delete("/api/v1/reports/%d/records/%d".formatted(id, expenseId)), alice, null, status().isOk()));
        assertThat(detail.get("riskScore").asInt()).isZero();
        assertThat(detail.get("totalExpense").decimalValue()).isEqualByComparingTo("0");
        addRecord(alice, id, "EXPENSE", "Rent", "400");

        // rascunho de outra pessoa não existe para bob
        call(get("/api/v1/reports/" + id), bob, null, status().isNotFound());
        call(post("/api/v1/reports/" + id + "/records"), bob,
                "{\"recordDate\":\"2026-03-10\",\"type\":\"INCOME\",\"category\":\"x\",\"amount\":1}",
                status().isNotFound());

        detail = body(call(post("/api/v1/reports/" + id + "/submit"), alice, null, status().isOk()));
        assertThat(detail.get("status").asText()).isEqualTo("PENDING");
        assertThat(detail.at("/can/edit").asBoolean()).isFalse();
        call(put("/api/v1/reports/" + id), alice, "{\"title\":\"x\",\"period\":\"2026-03\"}", status().isConflict());
        call(post("/api/v1/reports/" + id + "/approve"), alice, null, status().isForbidden());

        detail = body(call(post("/api/v1/reports/" + id + "/approve"), admin, "{\"note\":\"ok\"}", status().isOk()));
        assertThat(detail.get("status").asText()).isEqualTo("APPROVED");
        call(post("/api/v1/reports/" + id + "/approve"), admin, null, status().isConflict());

        // aprovado fica visível para todos, mas só leitura
        JsonNode seenByBob = body(call(get("/api/v1/reports/" + id), bob, null, status().isOk()));
        assertThat(seenByBob.at("/can/edit").asBoolean()).isFalse();
        call(delete("/api/v1/reports/" + id), bob, null, status().isForbidden());
        call(delete("/api/v1/reports/" + id), alice, null, status().isForbidden());

        JsonNode audit = body(call(get("/api/v1/admin/audit-logs?size=200"), admin, null, status().isOk()));
        assertThat(audit.get("content").findValuesAsText("action"))
                .contains("REPORT_CREATED", "RECORD_ADDED", "RECORD_DELETED", "REPORT_SUBMITTED", "REPORT_APPROVED");
    }

    @Test
    void rejectionNeedsANoteAndSendsReportBackToOwner() throws Exception {
        long id = createReport(alice, "To reject");
        call(post("/api/v1/reports/" + id + "/submit"), alice, null, status().isUnprocessableEntity()); // sem lançamentos
        addRecord(alice, id, "INCOME", "Sales", "10");
        call(post("/api/v1/reports/" + id + "/submit"), alice, null, status().isOk());

        call(post("/api/v1/reports/" + id + "/reject"), admin, "{\"note\":\" \"}", status().isUnprocessableEntity());
        JsonNode r = body(call(post("/api/v1/reports/" + id + "/reject"), admin, "{\"note\":\"Missing payroll\"}", status().isOk()));
        assertThat(r.get("status").asText()).isEqualTo("REJECTED");
        assertThat(r.get("reviewNote").asText()).isEqualTo("Missing payroll");

        r = body(call(put("/api/v1/reports/" + id), alice, "{\"title\":\"Fixed\",\"period\":\"2026-Q1\"}", status().isOk()));
        assertThat(r.get("status").asText()).isEqualTo("DRAFT");
    }

    @Test
    void adminCannotReviewOwnReport() throws Exception {
        long id = createReport(admin, "Admin own");
        addRecord(admin, id, "INCOME", "Sales", "10");
        call(post("/api/v1/reports/" + id + "/submit"), admin, null, status().isOk());
        call(post("/api/v1/reports/" + id + "/approve"), admin, null, status().isForbidden());
    }

    @Test
    void viewersCannotCreateAndAnalystsCannotAdminister() throws Exception {
        call(get("/api/v1/admin/users"), alice, null, status().isForbidden());
        long id = userId(me(bob));
        call(patch("/api/v1/admin/users/" + id), admin, "{\"role\":\"USER\"}", status().isOk());
        call(post("/api/v1/reports"), bob, "{\"title\":\"x\",\"period\":\"2026-01\"}", status().isForbidden());
        call(patch("/api/v1/admin/users/" + userId("admin")), admin, "{\"role\":\"USER\"}", status().isForbidden());
    }

    @Test
    void invalidPayloadsAre400() throws Exception {
        call(post("/api/v1/reports"), alice, "{\"title\":\"x\",\"period\":\"2026-13\"}", status().isBadRequest());
        long id = createReport(alice, "Validation");
        for (String amount : new String[] {"0", "-5", "1.001", "12345678901234"}) {
            call(post("/api/v1/reports/" + id + "/records"), alice,
                    "{\"recordDate\":\"2026-03-10\",\"type\":\"EXPENSE\",\"category\":\"x\",\"amount\":%s}".formatted(amount),
                    status().isBadRequest());
        }
        call(post("/api/v1/reports/" + id + "/records"), alice,
                "{\"recordDate\":\"2026-03-10\",\"type\":\"LOAN\",\"category\":\"x\",\"amount\":1}", status().isBadRequest());
        call(get("/api/v1/reports?status=NOPE"), alice, null, status().isBadRequest());
    }

    @Test
    void listFiltersAndAnalyticsOnlyCoverVisibleReports() throws Exception {
        String carol = register("carol");
        long mineId = createReport(carol, "Carol 100% fuel");
        addRecord(carol, mineId, "EXPENSE", "Fuel", "50");
        createReport(alice, "Alice hidden draft");

        JsonNode page = body(call(get("/api/v1/reports?mine=true"), carol, null, status().isOk()));
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        page = body(call(get("/api/v1/reports?q=hidden"), carol, null, status().isOk()));
        assertThat(page.get("totalElements").asLong()).isZero();
        page = body(call(get("/api/v1/reports?q=hidden"), admin, null, status().isOk()));
        assertThat(page.get("totalElements").asLong()).isPositive();
        page = body(call(get("/api/v1/reports?riskLevel=CRITICAL&mine=true"), carol, null, status().isOk()));
        assertThat(page.findValuesAsText("title")).containsExactly("Carol 100% fuel");

        JsonNode summary = body(call(get("/api/v1/analytics/summary"), carol, null, status().isOk()));
        assertThat(summary.at("/byStatus/DRAFT").asLong()).isEqualTo(1);
        assertThat(summary.at("/byRiskLevel/CRITICAL").asLong()).isEqualTo(1);
        assertThat(summary.get("topRisks").findValuesAsText("title")).contains("Carol 100% fuel");
    }

    @Test
    void corsPreflightAllowsOnlyTheConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/v1/reports").header("Origin", "https://frontend.test")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.test"));
        mvc.perform(options("/api/v1/reports").header("Origin", "https://evil.test")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthAndDocsArePublic() throws Exception {
        call(get("/actuator/health"), null, null, status().isOk());
        call(get("/api/v1/docs"), null, null, status().isOk());
    }
}
