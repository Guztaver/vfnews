package com.vfnews.factchecker.service.google;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GoogleFactCheckService {

    private final RestClient restClient;

    @Value("${google.factcheck.api.key:}")
    private String apiKey;

    public GoogleFactCheckService() {
        this.restClient = RestClient.builder()
            .baseUrl("https://factchecktools.googleapis.com/v1alpha1")
            .build();
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
                        if (currentToken != null && !currentToken.isEmpty()) {
                            uriBuilder.queryParam("pageToken", currentToken);
                        }
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
                            GoogleFactCheckResponse.ClaimReview review = claim
                                .getClaimReview()
                                .getFirst();
                            Map<String, String> result = new HashMap<>();
                            result.put("text", claim.getText());
                            result.put("claimant", claim.getClaimant());
                            result.put(
                                "publisher",
                                review.getPublisher() != null
                                    ? review.getPublisher().getName()
                                    : null
                            );
                            result.put("rating", review.getTextualRating());
                            result.put("url", review.getUrl());
                            results.add(result);
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
            log.error(
                "Error in bulk search for query '{}': {}",
                query,
                e.getMessage()
            );
        }

        return results;
    }

    public Map<String, String> checkClaim(String claim) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

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
                GoogleFactCheckResponse.Claim claimData = response
                    .getClaims()
                    .getFirst();
                if (
                    claimData.getClaimReview() != null &&
                    !claimData.getClaimReview().isEmpty()
                ) {
                    GoogleFactCheckResponse.ClaimReview review = claimData
                        .getClaimReview()
                        .getFirst();
                    Map<String, String> result = new HashMap<>();
                    result.put("text", claimData.getText());
                    result.put("claimant", claimData.getClaimant());
                    result.put(
                        "publisher",
                        review.getPublisher() != null
                            ? review.getPublisher().getName()
                            : null
                    );
                    result.put("rating", review.getTextualRating());
                    result.put("url", review.getUrl());
                    return result;
                }
            }
        } catch (Exception e) {
            // Log error
            log.error(
                "Error querying Google Fact Check API: " + e.getMessage()
            );
        }

        return null;
    }
}
