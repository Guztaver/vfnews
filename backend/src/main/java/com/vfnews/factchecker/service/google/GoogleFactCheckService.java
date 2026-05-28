package com.vfnews.factchecker.service.google;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GoogleFactCheckService {

    private final RestClient restClient;
    private final Set<String> trustedPublishers;

    @Value("${google.factcheck.api.key:}")
    private String apiKey;

    public GoogleFactCheckService(
        @Value("${google.factcheck.trusted-publishers:}") List<
            String
        > publishers
    ) {
        this.restClient = RestClient.builder()
            .baseUrl("https://factchecktools.googleapis.com/v1alpha1")
            .build();
        this.trustedPublishers =
            publishers != null
                ? publishers
                      .stream()
                      .map(String::trim)
                      .map(String::toLowerCase)
                      .filter(s -> !s.isEmpty())
                      .collect(Collectors.toSet())
                : Set.of();
        log.info(
            "Loaded {} trusted publishers for fact-check filtering",
            trustedPublishers.size()
        );
    }

    private boolean isTrusted(String publisherName) {
        if (publisherName == null || publisherName.isBlank()) return false;
        String lower = publisherName.toLowerCase().trim();
        for (String trusted : trustedPublishers) {
            if (lower.contains(trusted) || trusted.contains(lower)) return true;
        }
        return false;
    }

    public List<Map<String, String>> searchBulk(String query, int maxResults) {
        List<Map<String, String>> results = new ArrayList<>();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn(
                "Google Fact Check API key not set. Skipping bulk search."
            );
            return results;
        }

        String pageToken = null;
        int skipped = 0;

        try {
            do {
                final String currentToken = pageToken;
                GoogleFactCheckResponse response = restClient
                    .get()
                    .uri(uriBuilder -> {
                        uriBuilder
                            .path("/claims:search")
                            .queryParam("query", query)
                            .queryParam("key", apiKey)
                            .queryParam("languageCode", "pt-BR")
                            .queryParam(
                                "pageSize",
                                Math.min(maxResults - results.size(), 100)
                            );
                        if (
                            currentToken != null && !currentToken.isEmpty()
                        ) uriBuilder.queryParam("pageToken", currentToken);
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(GoogleFactCheckResponse.class);

                if (response != null && response.getClaims() != null) {
                    for (GoogleFactCheckResponse.Claim claim : response.getClaims()) {
                        if (
                            claim.getClaimReview() != null &&
                            !claim.getClaimReview().isEmpty()
                        ) {
                            var review = claim.getClaimReview().getFirst();
                            String publisher =
                                review.getPublisher() != null
                                    ? review.getPublisher().getName()
                                    : null;
                            if (!isTrusted(publisher)) {
                                skipped++;
                                continue;
                            }
                            Map<String, String> r = new HashMap<>();
                            r.put("text", claim.getText());
                            r.put("claimant", claim.getClaimant());
                            r.put("publisher", publisher);
                            r.put("rating", review.getTextualRating());
                            r.put("url", review.getUrl());
                            results.add(r);
                            if (results.size() >= maxResults) break;
                        }
                    }
                    pageToken = response.getNextPageToken();
                } else {
                    pageToken = null;
                }
            } while (
                pageToken != null &&
                !pageToken.isEmpty() &&
                results.size() < maxResults
            );
        } catch (Exception e) {
            log.error("Error in bulk search '{}': {}", query, e.getMessage());
        }

        log.info(
            "Bulk '{}': {} trusted, {} skipped",
            query,
            results.size(),
            skipped
        );
        return results;
    }

    public Map<String, String> checkClaim(String claim) {
        if (apiKey == null || apiKey.isEmpty()) return null;

        try {
            GoogleFactCheckResponse response = restClient
                .get()
                .uri(uriBuilder ->
                    uriBuilder
                        .path("/claims:search")
                        .queryParam("query", claim)
                        .queryParam("key", apiKey)
                        .queryParam("languageCode", "pt-BR")
                        .build()
                )
                .retrieve()
                .body(GoogleFactCheckResponse.class);

            if (
                response != null &&
                response.getClaims() != null &&
                !response.getClaims().isEmpty()
            ) {
                Map<String, String> fallback = null;

                for (GoogleFactCheckResponse.Claim claimData : response.getClaims()) {
                    if (
                        claimData.getClaimReview() != null &&
                        !claimData.getClaimReview().isEmpty()
                    ) {
                        var review = claimData.getClaimReview().getFirst();
                        String publisher =
                            review.getPublisher() != null
                                ? review.getPublisher().getName()
                                : null;

                        Map<String, String> result = new HashMap<>();
                        result.put("text", claimData.getText());
                        result.put("claimant", claimData.getClaimant());
                        result.put("publisher", publisher);
                        result.put("rating", review.getTextualRating());
                        result.put("url", review.getUrl());

                        if (isTrusted(publisher)) return result;
                        if (fallback == null) fallback = result;
                    }
                }

                // Return first available result even if not in whitelist
                if (fallback != null) return fallback;
            }
        } catch (Exception e) {
            log.error(
                "Error querying Google Fact Check API: {}",
                e.getMessage()
            );
        }

        return null;
    }
}
