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

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.model:openai/gpt-4o-mini}")
    private String model;

    @Value("${openrouter.url:https://api.openrouter.ai/v1/chat/completions}")
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

            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.exchange(openrouterUrl, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("OpenRouter non-2xx: {}", resp.getStatusCode());
                return new LlmResult("unknown", "", "{}");
            }
            JsonNode root = mapper.readTree(resp.getBody());
            // expected path: choices[0].message.content
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                log.warn("Empty LLM content");
                return new LlmResult("unknown", "", "{}");
            }

            // Try parse content as JSON
            try {
                JsonNode extracted = mapper.readTree(content);
                String docType = extracted.path("documentType").asText("unknown");
                String summary = extracted.path("summary").asText("");
                JsonNode entities = extracted.path("entities");
                String entitiesJson = entities.isMissingNode() ? "{}" : mapper.writeValueAsString(entities);
                return new LlmResult(docType, summary, entitiesJson);
            } catch (Exception e) {
                // best-effort: attempt to extract json substring
                int idx = content.indexOf('{');
                if (idx >= 0) {
                    String sub = content.substring(idx);
                    try {
                        JsonNode extracted = mapper.readTree(sub);
                        String docType = extracted.path("documentType").asText("unknown");
                        String summary = extracted.path("summary").asText("");
                        JsonNode entities = extracted.path("entities");
                        String entitiesJson = entities.isMissingNode() ? "{}" : mapper.writeValueAsString(entities);
                        return new LlmResult(docType, summary, entitiesJson);
                    } catch (Exception ex2) {
                        log.warn("Failed to parse partial JSON from LLM: {}", ex2.getMessage());
                        return new LlmResult("unknown", content, "{}");
                    }
                } else {
                    return new LlmResult("unknown", content, "{}");
                }
            }

        } catch (Exception e) {
            log.warn("LLM call failed: {}", e.getMessage());
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
