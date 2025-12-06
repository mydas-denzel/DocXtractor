package com.hng.docxtractor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.docxtractor.dto.LlmDocumentAnalysisRequest;
import com.hng.docxtractor.dto.LlmDocumentAnalysisResponse;
import com.hng.docxtractor.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenRouterLlmServiceImpl implements LlmService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    private static final String OPENROUTER_URL = "https://api.openrouter.ai/v1/chat/completions";

    @Override
    public LlmDocumentAnalysisResponse analyzeDocument(LlmDocumentAnalysisRequest request) {
        try {
            String prompt = buildPrompt(request);

            String bodyJson = "{\n" +
                    "  \"model\": \"" + model + "\",\n" +
                    "  \"messages\": [{\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}]\n" +
                    "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    OPENROUTER_URL, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("OpenRouter API returned status {}", response.getStatusCode());
                return defaultResponse();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("choices").get(0).path("message").path("content").asText();

            return parseLlmOutput(text);

        } catch (Exception e) {
            log.warn("LLM analysis failed: {}", e.getMessage());
            return defaultResponse();
        }
    }

    private String buildPrompt(LlmDocumentAnalysisRequest req) {
        return """
                You are a document analyzer. Given the extracted text and metadata below,
                classify the document type (e.g., invoice, CV, report, letter),
                provide a concise summary in 2-3 sentences,
                and extract structured entities (names, dates, amounts, emails, phones).
                Respond in JSON only with fields: documentType, summary, entities.

                Metadata:
                File Name: %s
                File Type: %s
                Has Images: %s
                Image Count: %d

                Extracted Text:
                %s
                """.formatted(
                req.getFileName(),
                req.getFileType(),
                req.isHasImages(),
                req.getImageCount(),
                req.getExtractedText().replace("\"", "\\\"").replace("\n","\\n")
        );
    }

    private String escapeJson(String input) {
        return input.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private LlmDocumentAnalysisResponse defaultResponse() {
        return LlmDocumentAnalysisResponse.builder()
                .documentType("unknown")
                .summary("")
                .entities(LlmDocumentAnalysisResponse.Entities.builder()
                        .names(new String[]{})
                        .dates(new String[]{})
                        .amounts(new String[]{})
                        .emails(new String[]{})
                        .phones(new String[]{})
                        .build())
                .build();
    }

    private LlmDocumentAnalysisResponse parseLlmOutput(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            JsonNode entities = root.path("entities");
            return LlmDocumentAnalysisResponse.builder()
                    .documentType(root.path("documentType").asText())
                    .summary(root.path("summary").asText())
                    .entities(LlmDocumentAnalysisResponse.Entities.builder()
                            .names(asStringArray(entities.path("names")))
                            .dates(asStringArray(entities.path("dates")))
                            .amounts(asStringArray(entities.path("amounts")))
                            .emails(asStringArray(entities.path("emails")))
                            .phones(asStringArray(entities.path("phones")))
                            .build())
                    .build();
        } catch (Exception e) {
            log.warn("Parsing LLM output failed: {}", e.getMessage());
            return defaultResponse();
        }
    }

    private String[] asStringArray(JsonNode node) {
        if (node == null || !node.isArray()) return new String[]{};
        return Arrays.stream(objectMapper.convertValue(node, String[].class)).toArray(String[]::new);
    }
}
