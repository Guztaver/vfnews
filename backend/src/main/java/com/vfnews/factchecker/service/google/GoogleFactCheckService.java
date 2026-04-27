package com.vfnews.factchecker.service.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleFactCheckService {

    private final RestClient restClient;

    @Value("${google.factcheck.api.key:}")
    private String apiKey;

    public GoogleFactCheckService() {
        this.restClient = RestClient.builder().baseUrl("https://factchecktools.googleapis.com/v1alpha1").build();
    }

    public Map<String, String> checkClaim(String claim) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        try {
            GoogleFactCheckResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/claims:search")
                            .queryParam("query", claim)
                            .queryParam("key", apiKey)
                            .queryParam("languageCode", "pt-BR")
                            .build())
                    .retrieve()
                    .body(GoogleFactCheckResponse.class);

            if (response != null && response.getClaims() != null && !response.getClaims().isEmpty()) {
                GoogleFactCheckResponse.Claim claimData = response.getClaims().getFirst();
                if (claimData.getClaimReview() != null && !claimData.getClaimReview().isEmpty()) {
                    GoogleFactCheckResponse.ClaimReview review = claimData.getClaimReview().getFirst();
                    Map<String, String> result = new HashMap<>();
                    result.put("text", claimData.getText());
                    result.put("claimant", claimData.getClaimant());
                    result.put("publisher", review.getPublisher() != null ? review.getPublisher().getName() : null);
                    result.put("rating", review.getTextualRating());
                    result.put("url", review.getUrl());
                    return result;
                }
            }
        } catch (Exception e) {
            // Log error
            System.err.println("Error querying Google Fact Check API: " + e.getMessage());
        }

        return null;
    }
}
