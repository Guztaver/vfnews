package com.vfnews.factchecker.service;

import com.vfnews.factchecker.config.DatasetSeederService;
import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.domain.FactCheck;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.repository.FactCheckRepository;
import com.vfnews.factchecker.service.google.GoogleFactCheckService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FactCheckService {

    private final FactCheckRepository repository;
    private final DatasetEntryRepository datasetEntryRepository;
    private final GoogleFactCheckService googleService;
    private final MLService mlService;

    public FactCheck checkClaim(String claimText) {
        // 1. Try Google Fact Check API
        Map<String, String> googleResult = googleService.checkClaim(claimText);
        if (googleResult != null) {
            FactCheck saved = repository.save(
                FactCheck.builder()
                    .claim(claimText)
                    .result(googleResult.get("rating"))
                    .source(FactCheck.Source.API)
                    .rating(googleResult.get("rating"))
                    .publisher(googleResult.get("publisher"))
                    .url(googleResult.get("url"))
                    .build()
            );

            // Also save as DatasetEntry for future ML training
            saveToDataset(claimText, googleResult.get("rating"));

            return saved;
        }

        // 2. Try Local ML Model
        Map<String, String> mlResult = mlService.predict(claimText);
        if (mlResult != null) {
            FactCheck saved = repository.save(
                FactCheck.builder()
                    .claim(claimText)
                    .result(mlResult.get("rating"))
                    .source(FactCheck.Source.ML)
                    .rating(mlResult.get("rating"))
                    .build()
            );

            // Also save as DatasetEntry for future ML training
            saveToDataset(claimText, mlResult.get("rating"));

            return saved;
        }

        return null;
    }

    /**
     * Save the checked claim as a DatasetEntry for future ML training.
     */
    private void saveToDataset(String text, String rating) {
        if (
            text != null &&
            !text.isBlank() &&
            !datasetEntryRepository.existsByText(text)
        ) {
            String label = DatasetSeederService.simplifyRating(rating);
            String keywords = extractKeywords(text);
            DatasetEntry entry = DatasetEntry.builder()
                .text(text)
                .label(label)
                .keywords(keywords)
                .build();
            datasetEntryRepository.save(entry);
        }
    }

    /**
     * Extract first few words from the claim text as keywords.
     */
    private String extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] words = text.trim().split("\\s+");
        int limit = Math.min(words.length, 4);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(" ");
            sb.append(words[i].replaceAll("[^\\p{L}]", "").toLowerCase());
        }
        return sb.toString();
    }
}
