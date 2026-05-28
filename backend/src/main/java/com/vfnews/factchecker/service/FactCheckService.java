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

    /** Record so the controller can surface the API-matched claim text. */
    public record CheckResult(FactCheck factCheck, String matchedText) {}

    public CheckResult checkClaim(String claimText) {
        Map<String, String> googleResult = googleService.checkClaim(claimText);
        if (googleResult != null) {
            String matched = googleResult.get("text");
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

            // Save matched claim text as dataset entry for ML training
            saveToDataset(
                matched != null ? matched : claimText,
                googleResult.get("rating"),
                googleResult.get("publisher")
            );

            return new CheckResult(saved, normalize(matched, claimText));
        }

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

            saveToDataset(claimText, mlResult.get("rating"), null);
            return new CheckResult(saved, null);
        }

        return null;
    }

    /** Returns null when the matched text equals the query (no mismatch to show). */
    private static String normalize(String matched, String query) {
        if (matched == null || matched.isBlank()) return null;
        String a = matched
            .trim()
            .toLowerCase()
            .replaceAll("[^\\p{L}\\d\\s]", "");
        String b = query.trim().toLowerCase().replaceAll("[^\\p{L}\\d\\s]", "");
        return a.equals(b) ? null : matched;
    }

    private void saveToDataset(String text, String rating, String publisher) {
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
                .publisher(publisher)
                .build();
            datasetEntryRepository.save(entry);
        }
    }

    private String extractKeywords(String text) {
        if (text == null || text.isBlank()) return "";
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
