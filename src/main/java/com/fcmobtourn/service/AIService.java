package com.fcmobtourn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate http = new RestTemplate();

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.model:openrouter/free}")
    private String model;

    public int analyzeSquadScreenshot(String image) {
        try {
            JsonNode result = ask(image, squadPrompt());
            int displayedOvr = result.path("displayedSquadOvr").asInt(-1);

            int total = 0;
            int count = 0;
            JsonNode ratings = result.path("startingPlayerRatings");
            if (ratings.isArray()) {
                for (JsonNode rating : ratings) {
                    int value = rating.asInt(-1);
                    if (value >= 1 && value <= 200) {
                        total += value;
                        count++;
                    }
                }
            }
            int calculatedOvr = count >= 8 ? Math.round((float) total / count) : -1;

            // Accept a displayed number only when the AI says it is explicitly labelled
            // as squad/team OVR, and when it is reasonably consistent with the XI.
            boolean explicit = result.path("ovrLabelFound").asBoolean(false);
            boolean plausible = calculatedOvr < 0 || Math.abs(displayedOvr - calculatedOvr) <= 5;
            if (explicit && displayedOvr >= 1 && displayedOvr <= 200 && plausible) {
                return displayedOvr;
            }
            if (calculatedOvr >= 1 && calculatedOvr <= 200) {
                return calculatedOvr;
            }
            if (explicit && displayedOvr >= 1 && displayedOvr <= 200) {
                return displayedOvr;
            }
        } catch (Exception ignored) {
            // Registration remains available if the vision provider is unavailable.
        }
        return 125;
    }

    public Map<String, Object> analyzeMatchScreenshot(String image, String home, String away) {
        try {
            JsonNode j = ask(image, """
                    Read the final score from this FC Mobile match-result screenshot.
                    The expected fixture is home team: %s and away team: %s.
                    Identify the displayed team names and scores. Set teamsMatch to true only
                    when both displayed teams clearly refer to those exact fixture teams.
                    Return JSON only: {"homeTeam":"","awayTeam":"","homeScore":0,"awayScore":0,"confidence":"high","teamsMatch":true}.
                    Use confidence low and teamsMatch false if any text is unreadable.
                    """.formatted(home, away));
            return Map.of(
                    "homeTeam", j.path("homeTeam").asText(""),
                    "awayTeam", j.path("awayTeam").asText(""),
                    "homeScore", j.path("homeScore").asInt(-1),
                    "awayScore", j.path("awayScore").asInt(-1),
                    "confidence", j.path("confidence").asText("low"),
                    "teamsMatch", j.path("teamsMatch").asBoolean(false));
        } catch (Exception e) {
            return Map.of("homeScore", -1, "awayScore", -1, "confidence", "low", "teamsMatch", false);
        }
    }

    private String squadPrompt() {
        return """
                Analyze this FC Mobile squad screenshot carefully. Return JSON only using this schema:
                {"displayedSquadOvr": 0, "ovrLabelFound": false, "startingPlayerRatings": [], "confidence": "high", "evidence": ""}

                Rules:
                1. Set displayedSquadOvr only when you can read a number next to an explicit label
                   such as OVR, TEAM OVR, SQUAD OVR, or OVERALL. Set ovrLabelFound true only then.
                2. Never treat a player-card rating, chemistry, rank, level, formation, coins,
                   currency, or jersey number as squad OVR. A large number alone is not enough.
                3. Read every visible starting-XI player-card rating into startingPlayerRatings.
                   Ignore substitutes, reserves, chemistry, and card-level numbers.
                4. If there is no clearly labelled squad OVR, set displayedSquadOvr to 0 and let
                   the application calculate a conservative starting-XI average.
                5. Use confidence high only when the labelled squad OVR is clearly readable.
                6. Do not invent values. Use integers only.
                """;
    }

    private JsonNode ask(String image, String instruction) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is not configured");
        }
        String data = image.startsWith("data:") ? image : "data:image/jpeg;base64," + image;
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "max_tokens", 300,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", instruction),
                                Map.of("type", "image_url", "image_url", Map.of("url", data))
                        )
                ))
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String raw = http.postForEntity(
                "https://openrouter.ai/api/v1/chat/completions",
                new HttpEntity<>(body, headers),
                String.class
        ).getBody();
        String content = mapper.readTree(raw).path("choices").path(0).path("message").path("content").asText();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Vision response did not contain JSON");
        }
        return mapper.readTree(content.substring(start, end + 1));
    }
}
