package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.activity.ActivityEventType;
import com.youkeda.exercise.claw.agent.activity.AgentActivityEvent;
import com.youkeda.exercise.claw.agent.activity.AgentActivityStore;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.bot.BotSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginPageServerTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LoginStateManager stateManager;
    private AgentActivityStore activityStore;
    private LoginPageServer server;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:sqlite::memory:", true);
        activityStore = new AgentActivityStore(new JdbcTemplate(dataSource));
        activityStore.init();

        BotSessionManager botSessionManager = new BotSessionManager(
                tempDir.resolve("bot-session.db").toString());
        botSessionManager.init();

        stateManager = new LoginStateManager();
        stateManager.updateQrUrl("https://example.test/qr");
        stateManager.updateStatus(LoginStatus.WAITING_SCAN);

        server = new LoginPageServer(
                stateManager, botSessionManager, activityStore, objectMapper);
        int port = server.start();
        baseUrl = "http://127.0.0.1:" + port;
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop();
    }

    @Test
    void showsFocusedLoginPageBeforeScan() throws Exception {
        HttpResponse<String> response = get("/login");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("连接微信"));
        assertTrue(response.body().contains("扫描二维码"));
        assertTrue(response.body().contains("/login/status"));
    }

    @Test
    void redirectsToDashboardAfterSuccessfulLogin() throws Exception {
        stateManager.updateStatus(LoginStatus.SUCCESS);

        HttpResponse<String> response = get("/login");

        assertEquals(302, response.statusCode());
        assertEquals("/dashboard", response.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void dashboardAndActivityApiExposeExecutionTimeline() throws Exception {
        stateManager.updateStatus(LoginStatus.SUCCESS);
        activityStore.record(new AgentActivityEvent(
                "request-7", ActivityEventType.SKILL_SELECTED,
                "weather", null, "SUCCESS", "选择 weather Skill", null));
        activityStore.record(new AgentActivityEvent(
                "request-7", ActivityEventType.TOOL_SUCCEEDED,
                "weather", "weather_query", "SUCCESS", "工具执行成功", 86L));

        HttpResponse<String> dashboard = get("/dashboard");
        HttpResponse<String> api = get("/api/activities?limit=20");
        JsonNode activities = objectMapper.readTree(api.body());

        assertEquals(200, dashboard.statusCode());
        assertTrue(dashboard.body().contains("活动记录"));
        assertTrue(dashboard.body().contains("Skills"));
        assertTrue(dashboard.body().contains("Tools"));
        assertEquals(200, api.statusCode());
        assertEquals("weather_query", activities.get(0).get("toolName").asText());
        assertEquals("weather", activities.get(1).get("skillName").asText());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
