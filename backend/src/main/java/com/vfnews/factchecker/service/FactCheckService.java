package com.vfnews.factchecker.service;

import com.vfnews.factchecker.config.DatasetSeederService;
import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.domain.FactCheck;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.repository.FactCheckRepository;
import com.vfnews.factchecker.service.google.GoogleFactCheckService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FactCheckService {

    private final FactCheckRepository repository;
    private final DatasetEntryRepository datasetEntryRepository;
    private final GoogleFactCheckService googleService;
    private final MLService mlService;

    public record CheckResult(FactCheck factCheck, String matchedText) {}

    public CheckResult checkClaim(String claimText) {
        Map<String, String> googleResult = googleService.checkClaim(claimText);
        if (googleResult != null) {
            String matched = googleResult.get("text");

            if (!isRelevant(claimText, matched)) {
                log.info(
                    "API matched unrelated claim '{}' for query '{}' — falling back to ML",
                    matched,
                    claimText
                );
                // fall through to ML below
            } else {
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

                saveToDataset(
                    matched != null ? matched : claimText,
                    googleResult.get("rating"),
                    googleResult.get("publisher")
                );

                return new CheckResult(saved, normalize(matched, claimText));
            }
        }

        Map<String, String> mlResult = mlService.predict(claimText);
        if (mlResult != null) {
            boolean inconclusive = "Inconclusivo".equals(
                mlResult.get("rating")
            );

            FactCheck saved = repository.save(
                FactCheck.builder()
                    .claim(claimText)
                    .result(
                        inconclusive
                            ? "Não foi possível determinar a veracidade desta alegação com os dados disponíveis."
                            : mlResult.get("rating")
                    )
                    .source(FactCheck.Source.ML)
                    .rating(mlResult.get("rating"))
                    .build()
            );

            if (!inconclusive) {
                saveToDataset(claimText, mlResult.get("rating"), null);
            }
            return new CheckResult(saved, null);
        }

        // Ultimate fallback — always return something friendly
        FactCheck fallback = repository.save(
            FactCheck.builder()
                .claim(claimText)
                .result(
                    "Não encontrei informações suficientes para verificar esta alegação. " +
                        "Tente reformular a pergunta com mais detalhes ou consulte fontes oficiais como o TSE."
                )
                .source(FactCheck.Source.ML)
                .rating("Inconclusivo")
                .build()
        );
        return new CheckResult(fallback, null);
    }

    /**
     * Returns true if the matched claim is relevant to the user's query.
     * Uses token overlap — at least 30% of query tokens must appear in the matched claim.
     */
    private boolean isRelevant(String query, String matched) {
        if (matched == null || matched.isBlank()) return true; // no matched text = assume relevant

        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return true;

        Set<String> matchedTokens = tokenize(matched);

        int overlap = 0;
        for (String t : queryTokens) {
            if (matchedTokens.contains(t)) overlap++;
        }

        if (overlap < 2 && queryTokens.size() >= 2) return false;
        double ratio = (double) overlap / queryTokens.size();
        return ratio >= 0.5;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        for (String word : text
            .toLowerCase()
            .replaceAll("[^\\p{L}\\d\\s]", " ")
            .split("\\s+")) {
            if (word.length() >= 3 && !STOPWORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private static final Set<String> STOPWORDS = Set.of(
        "que",
        "com",
        "para",
        "uma",
        "isso",
        "esse",
        "essa",
        "este",
        "esta",
        "dos",
        "das",
        "por",
        "como",
        "mas",
        "foi",
        "era",
        "sao",
        "tem",
        "the",
        "and",
        "for",
        "are",
        "was",
        "not",
        "you",
        "all",
        "can",
        "had",
        "her",
        "one",
        "our",
        "out",
        "has",
        "have",
        "from",
        "they",
        "this",
        "that",
        "will",
        "what",
        "when",
        "where",
        "which",
        "with",
        "about",
        "each"
    );

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
