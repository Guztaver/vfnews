package com.vfnews.factchecker.config;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.service.google.GoogleFactCheckService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetSeederService {

    private final GoogleFactCheckService googleFactCheckService;
    private final DatasetEntryRepository repository;

    private static final List<String> KEYWORDS = List.of(
        "eleição",
        "bolsonaro",
        "lula",
        "pt",
        "campanha",
        "urna",
        "voto",
        "fraude"
    );

    private static final int MAX_PER_KEYWORD = 20;

    /**
     * Seeds the dataset by querying the Google Fact Check API for each keyword.
     * Returns the list of newly added DatasetEntries.
     */
    public List<DatasetEntry> seedFromApi() {
        List<DatasetEntry> added = new ArrayList<>();

        for (String keyword : KEYWORDS) {
            log.info(
                "Searching Google Fact Check API for keyword: {}",
                keyword
            );
            List<Map<String, String>> results =
                googleFactCheckService.searchBulk(keyword, MAX_PER_KEYWORD);

            for (Map<String, String> result : results) {
                String text = result.get("text");
                if (text == null || text.isBlank()) {
                    continue;
                }

                // Skip duplicates
                if (repository.existsByText(text)) {
                    continue;
                }

                String rating = result.get("rating");
                String label = simplifyRating(rating);

                DatasetEntry entry = DatasetEntry.builder()
                    .text(text)
                    .label(label)
                    .keywords(keyword)
                    .build();
                repository.save(entry);
                added.add(entry);
            }
        }

        log.info(
            "DatasetSeederService: added {} new entries from Google Fact Check API.",
            added.size()
        );
        return added;
    }

    /**
     * Simplifies a fact-check rating into one of three labels:
     * "false", "true", or "mixed".
     */
    public static String simplifyRating(String rating) {
        if (rating == null) {
            return "mixed";
        }
        String lower = rating.toLowerCase();
        if (
            lower.contains("falso") ||
            lower.contains("false") ||
            lower.contains("fake") ||
            lower.contains("mentira") ||
            lower.contains("engano")
        ) {
            return "false";
        }
        if (
            lower.contains("verdade") ||
            lower.contains("true") ||
            lower.contains("real")
        ) {
            return "true";
        }
        return "mixed";
    }
}
