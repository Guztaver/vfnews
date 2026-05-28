package com.vfnews.factchecker.config;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports the FACTCK.BR dataset (https://github.com/jghm-f/FACTCK.BR)
 * into the DatasetEntry table.
 *
 * The TSV has columns: URL, Author, datePublished, claimReviewed,
 * reviewBody, title, ratingValue, bestRating, alternativeName
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FACTCKBRImporterService {

    private final DatasetEntryRepository repository;

    private static final String TSV_PATH = "datasets/FACTCKBR.tsv";

    /** Extracts a clean publisher name from the Author URL. */
    private static final Map<String, String> AUTHOR_MAP = Map.of(
        "https:www.aosfatos.org",
        "Aos Fatos",
        "https:piaui.folha.uol.com.brlupa",
        "Lupa",
        "https:apublica.org",
        "Agência Pública (Truco)"
    );

    /**
     * Imports all entries from the FACTCK.BR TSV file.
     * Returns the number of new entries added.
     */
    @Transactional
    public int importFromTSV() {
        List<DatasetEntry> batch = new ArrayList<>();
        int skipped = 0;
        int imported = 0;

        try (
            InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream(TSV_PATH);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
            )
        ) {
            if (is == null) {
                log.warn(
                    "FACTCKBR.tsv not found at classpath:{}. Skipping import.",
                    TSV_PATH
                );
                return 0;
            }

            // Skip header line
            String header = reader.readLine();
            if (header == null) {
                log.warn("FACTCKBR.tsv is empty.");
                return 0;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split("\t", -1);
                if (cols.length < 9) continue;

                String claimText = cols[3]; // claimReviewed
                String author = cols[1]; // Author (URL)
                String altName = cols[8]; // alternativeName
                String title = cols[5]; // title
                String reviewBody = cols[4]; // reviewBody

                if (claimText == null || claimText.isBlank()) continue;

                // Use the first 500 chars as the text to store (full claims can be very long)
                String text = truncate(claimText, 500);

                if (repository.existsByText(text)) {
                    skipped++;
                    continue;
                }

                String label = simplifyLabel(altName);
                String publisher = mapPublisher(author);
                String keywords = extractKeywords(
                    text + " " + title + " " + reviewBody
                );

                DatasetEntry entry = DatasetEntry.builder()
                    .text(text)
                    .label(label)
                    .keywords(keywords)
                    .publisher(publisher)
                    .build();

                batch.add(entry);

                if (batch.size() >= 100) {
                    repository.saveAll(batch);
                    imported += batch.size();
                    batch.clear();
                }
            }

            // Flush remaining
            if (!batch.isEmpty()) {
                repository.saveAll(batch);
                imported += batch.size();
            }

            log.info(
                "FACTCK.BR: imported {} entries, skipped {} duplicates.",
                imported,
                skipped
            );
        } catch (IOException e) {
            log.error("Error reading FACTCKBR.tsv: {}", e.getMessage(), e);
        }

        return imported;
    }

    /**
     * Maps FACTCK.BR rating labels to one of: "false", "true", "mixed".
     */
    static String simplifyLabel(String label) {
        if (label == null) return "mixed";
        String lower = label.toLowerCase().trim();

        if (
            lower.equals("falso") ||
            lower.equals("false") ||
            lower.equals("fake") ||
            lower.equals("mentira") ||
            lower.equals("engano")
        ) {
            return "false";
        }

        if (
            lower.equals("verdadeiro") ||
            lower.equals("true") ||
            lower.equals("real")
        ) {
            return "true";
        }

        // Everything else: distorcido, exagerado, insustentável, impreciso,
        // subestimado, discutível, impossível provar, sem contexto, etc.
        return "mixed";
    }

    /**
     * Extracts a clean publisher name from the author URL column.
     */
    private String mapPublisher(String author) {
        if (author == null || author.isBlank()) return "Desconhecido";
        return AUTHOR_MAP.getOrDefault(author, "FACTCK.BR");
    }

    /**
     * Extracts up to 3 keywords from the text based on keyword presence.
     */
    private String extractKeywords(String text) {
        if (text == null || text.isBlank()) return "factcheck";

        String lower = text.toLowerCase();
        List<String> found = new ArrayList<>();

        for (String kw : KEYWORD_PATTERNS) {
            String cleaned = kw.replace("_", " ");
            if (lower.contains(cleaned) && !found.contains(cleaned)) {
                found.add(cleaned);
                if (found.size() >= 5) break;
            }
        }

        return found.isEmpty() ? "factcheck, brasil" : String.join(", ", found);
    }

    private static final List<String> KEYWORD_PATTERNS = List.of(
        "eleição",
        "eleitor",
        "urna",
        "voto",
        "fraude",
        "bolsonaro",
        "lula",
        "dilma",
        "temer",
        "haddad",
        "pt",
        "psdb",
        "psl",
        "mdb",
        "psol",
        "stf",
        "tse",
        "congresso",
        "câmara",
        "senado",
        "previdência",
        "reforma",
        "saúde",
        "educação",
        "vacina",
        "covid",
        "corrupção",
        "lava_jato",
        "polícia",
        "segurança",
        "exército",
        "moro",
        "fake_news",
        "whatsapp",
        "facebook",
        "twitter",
        "desmatamento",
        "amazônia",
        "marina",
        "impeachment",
        "constituição",
        "supremo"
    );

    private static String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        // Try to break at a space
        int cut = text.lastIndexOf(' ', maxLen);
        return text.substring(0, cut > 0 ? cut : maxLen);
    }
}
