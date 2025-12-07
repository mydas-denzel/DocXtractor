package com.hng.docxtractor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.docxtractor.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenRouterLlmServiceImpl implements LlmService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.url}")
    private String openrouterUrl;

    @Override
    public LlmResult analyze(String fileName, String fileType, String extractedText, boolean hasImages, int imageCount) {
        try {
            String prompt = buildPrompt(fileName, fileType, extractedText, hasImages, imageCount);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);

            List<Map<String,String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) headers.setBearerAuth(apiKey);
            // required by OpenRouter
            headers.set("HTTP-Referer", "http://localhost");
            headers.set("X-Title", "DocXtractor");

            String bodyJson = mapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            ResponseEntity<String> resp = restTemplate.exchange(openrouterUrl, HttpMethod.POST, entity, String.class);

            // Log status & headers for debugging (important)
            log.debug("OpenRouter response status: {}, headers: {}", resp.getStatusCode(), resp.getHeaders());

            String respBody = resp.getBody();
            if (respBody == null) {
                log.warn("OpenRouter returned empty body (status {})", resp.getStatusCode());
                return new LlmResult("unknown", "", "{}");
            }

            // If server returned non-2xx, log body and bail
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("OpenRouter non-2xx {}: {}", resp.getStatusCode(), respBody);
                // If the body looks like HTML, log an excerpt to help debugging
                if (respBody.trim().startsWith("<")) {
                    log.warn("OpenRouter returned HTML (likely an error page). First 500 chars:\n{}", respBody.length() > 500 ? respBody.substring(0,500) : respBody);
                }
                return new LlmResult("unknown", "", "{}");
            }

            // Defensive check: ensure Content-Type is JSON or body looks like JSON
            MediaType ct = resp.getHeaders().getContentType();
            boolean likelyJson = (ct != null && (ct.includes(MediaType.APPLICATION_JSON) || ct.getSubtype().contains("json")))
                    || respBody.trim().startsWith("{")
                    || respBody.trim().startsWith("[");

            if (!likelyJson) {
                log.warn("OpenRouter returned non-JSON response. Content-Type: {}, body-start: {}", ct, respBody.length()>100?respBody.substring(0,100):respBody);
                return new LlmResult("unknown", "", "{}");
            }

            // Parse the actual response JSON
            JsonNode root = mapper.readTree(respBody);
            String content = root.path("choices").path(0).path("message").path("content").asText(null);

            if (content == null || content.isBlank()) {
                log.warn("Empty LLM content in JSON response; full resp: {}", respBody.length()>1000?respBody.substring(0,1000):respBody);
                return new LlmResult("unknown", "", "{}");
            }

            // try parse LLM content (which should be JSON string)
            try {
                JsonNode extracted = mapper.readTree(content);
                String docType = extracted.path("documentType").asText("unknown");
                String summary = extracted.path("summary").asText("");
                JsonNode entities = extracted.path("entities");
                String entitiesJson = entities.isMissingNode() ? "{}" : mapper.writeValueAsString(entities);
                return new LlmResult(docType, summary, entitiesJson);
            } catch (Exception e) {
                log.warn("Failed to parse LLM content as JSON, returning raw content. parseErr={}", e.getMessage());
                // best-effort: try to find JSON substring
                int idx = content.indexOf('{');
                if (idx >= 0) {
                    try {
                        JsonNode extracted = mapper.readTree(content.substring(idx));
                        String docType = extracted.path("documentType").asText("unknown");
                        String summary = extracted.path("summary").asText("");
                        JsonNode entities = extracted.path("entities");
                        String entitiesJson = entities.isMissingNode() ? "{}" : mapper.writeValueAsString(entities);
                        return new LlmResult(docType, summary, entitiesJson);
                    } catch (Exception ex2) {
                        log.warn("Failed to parse partial JSON: {}", ex2.getMessage());
                        return new LlmResult("unknown", content, "{}");
                    }
                } else {
                    return new LlmResult("unknown", content, "{}");
                }
            }

        } catch (Exception e) {
            // Log full exception with stacktrace for diagnostics
            log.warn("LLM call failed: {}", e.getMessage(), e);
            return new LlmResult("unknown", "", "{}");
        }
    }

    private String buildPrompt(String fileName, String fileType, String text, boolean hasImages, int imageCount) {
        // Use a strict JSON-only response instruction and simple schema
        String escapedText = text == null ? "" : text.replace("\"", "\\\"").replace("\n", "\\n");
        return """
                You are a strict JSON-only document analyzer. Respond ONLY with a single valid JSON object and nothing else.
                The JSON must contain exactly these top-level fields:
                {
                  "documentType": string,   // one of: invoice, cv, resume, report, letter, email, receipt, contract, unknown
                  "summary": string,        // short 2-3 sentence summary
                  "entities": {             // extracted structured entities
                     "names": [string],
                     "dates": [string],
                     "amounts": [string],
                     "emails": [string],
                     "phones": [string]
                  }
                }
                Use the extracted text below. If you cannot find any of the entities, return empty arrays.
                Metadata:
                FileName: %s
                FileType: %s
                HasImages: %s
                ImageCount: %d

                ExtractedText:
                %s
                """.formatted(fileName, fileType, String.valueOf(hasImages), imageCount, escapedText);
    }
}
