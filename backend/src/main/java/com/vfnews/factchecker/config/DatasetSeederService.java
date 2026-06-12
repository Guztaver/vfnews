package com.vfnews.factchecker.config;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.service.google.GoogleFactCheckService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetSeederService {

    private final GoogleFactCheckService googleFactCheckService;
    private final DatasetEntryRepository repository;

    @Value("${google.factcheck.trusted-publishers:}")
    private List<String> trustedPublishers;

    private static final List<String> KEYWORDS = List.of(
        "eleicao",
        "bolsonaro",
        "lula",
        "pt",
        "campanha",
        "urna",
        "voto",
        "fraude"
    );

    private static final int MAX_PER_KEYWORD = 30;

    /**
     * Removes dataset entries that came from non-trusted publishers or have
     * no publisher set (old data from before this field existed).
     * Also removes Fake.br Corpus and ISOT Dataset entries because those
     * datasets classify entire articles (fake/real news), not individual
     * claims — using them for claim-level fact-checking corrupts the model.
     */
    @Transactional
    public int cleanUntrustedEntries() {
        List<DatasetEntry> all = repository.findAll();
        List<Long> toDelete = new ArrayList<>();

        for (DatasetEntry entry : all) {
            String pub = entry.getPublisher();
            if (pub == null || pub.isBlank()) {
                toDelete.add(entry.getId());
            } else if (
                ConsolidadoImporterService.isUntrusted(pub)
            ) {
                toDelete.add(entry.getId());
            }
        }

        if (!toDelete.isEmpty()) {
            repository.deleteAllById(toDelete);
            log.info(
                "Removed {} untrusted dataset entries (no publisher info or unsuitable source) — will re-seed from trusted sources",
                toDelete.size()
            );
        }
        return toDelete.size();
    }

    /**
     * Seeds the dataset by querying the Google Fact Check API for each keyword.
     * The API now filters by trusted publishers internally.
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
                if (text == null || text.isBlank()) continue;

                if (repository.existsByText(text)) continue;

                String rating = result.get("rating");
                String label = simplifyRating(rating);
                String publisher = result.get("publisher");

                DatasetEntry entry = DatasetEntry.builder()
                    .text(text)
                    .label(label)
                    .keywords(keyword)
                    .publisher(publisher)
                    .build();
                repository.save(entry);
                added.add(entry);
            }
        }

        log.info(
            "DatasetSeederService: added {} new entries from trusted publishers",
            added.size()
        );
        return added;
    }

    /**
     * Simplifies a fact-check rating into one of three labels:
     * "false", "true", or "mixed".
     */
    public static String simplifyRating(String rating) {
        if (rating == null) return "mixed";
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
