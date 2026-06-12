package com.vfnews.factchecker.config;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports the dataset_consolidado.csv into the DatasetEntry table.
 *
 * CSV columns: texto, label, fonte, data_coleta
 *
 * Fields may be quoted ("...") and may contain embedded newlines,
 * requiring a proper CSV state-machine parser.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidadoImporterService {

    private final DatasetEntryRepository repository;

    private static final String CSV_PATH = "datasets/dataset_consolidado.csv";

    /**
     * Imports all entries from the consolidated CSV file.
     * Returns the number of new entries added.
     */
    @Transactional
    public int importFromCSV() {
        List<DatasetEntry> batch = new ArrayList<>();
        int skipped = 0;
        int imported = 0;

        try (
            InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream(CSV_PATH);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)
            )
        ) {
            if (is == null) {
                log.warn(
                    "dataset_consolidado.csv not found at classpath:{}. Skipping.",
                    CSV_PATH
                );
                return 0;
            }

            // Read and skip header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("dataset_consolidado.csv is empty.");
                return 0;
            }

            // Read the entire content for state-machine CSV parsing
            StringBuilder raw = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                raw.append(line).append('\n');
            }

            List<String[]> rows = parseCSV(raw.toString());

            // Skip header row
            for (int i = 1; i < rows.size(); i++) {
                String[] cols = rows.get(i);
                if (cols.length < 3) continue;

                String texto = cols[0];
                String labelOrig = cols[1];
                String fonte = cols[2];

                if (texto == null || texto.isBlank()) continue;
                // Skip entries that look like CSV artifacts
                if (texto.startsWith("Via:") && texto.length() < 60) continue;

                // Skip publishers that are not actual claim-level fact-checking sources.
                // Fake.br Corpus and ISOT Dataset classify entire news articles
                // (fake/real news), NOT individual claims — using them for
                // claim-level fact-checking corrupts the model.
                if (isUntrustedPublisher(fonte)) continue;

                // Truncate long texts for the dataset entry
                String text = truncate(texto, 500);

                if (repository.existsByText(text)) {
                    skipped++;
                    continue;
                }

                String label = simplifyLabel(labelOrig);
                String publisher = cleanSource(fonte);
                String keywords = extractKeywords(texto);

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
                "Consolidado: imported {} entries, skipped {} duplicates.",
                imported,
                skipped
            );
        } catch (IOException e) {
            log.error(
                "Error reading dataset_consolidado.csv: {}",
                e.getMessage(),
                e
            );
        }

        return imported;
    }

    /**
     * Simple state-machine CSV parser that handles:
     * - Fields separated by comma
     * - Fields enclosed in double quotes
     * - Embedded newlines within quoted fields
     * - Escaped quotes ("" becomes ")
     */
    static List<String[]> parseCSV(String content) {
        List<String[]> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Check for escaped quote ("")
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++; // skip next quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(field.toString().trim());
                    field.setLength(0);
                } else if (c == '\n') {
                    currentRow.add(field.toString().trim());
                    field.setLength(0);
                    // Add row only if it has at least some content (skip empty trailing lines)
                    if (!currentRow.isEmpty() && hasContent(currentRow)) {
                        rows.add(currentRow.toArray(new String[0]));
                    }
                    currentRow = new ArrayList<>();
                } else if (c == '\r') {
                    // Skip carriage return
                } else {
                    field.append(c);
                }
            }
        }

        // Flush last field and row
        if (field.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(field.toString().trim());
            if (!currentRow.isEmpty() && hasContent(currentRow)) {
                rows.add(currentRow.toArray(new String[0]));
            }
        }

        return rows;
    }

    private static boolean hasContent(List<String> row) {
        return row.stream().anyMatch(s -> !s.isBlank());
    }

    /**
     * Maps consolidated CSV labels to one of: "false", "true", "mixed".
     */
    static String simplifyLabel(String label) {
        if (label == null) return "true";
        String lower = label.toLowerCase().trim();

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

        // Default: treat as "true" only if it looks like a real-news label
        return "true";
    }

    /**
     * Publishers whose datasets classify entire articles (fake/real news)
     * rather than individual claims — unsuitable for claim-level fact-checking.
     */
    private static final Set<String> UNTRUSTED_PUBLISHERS = Set.of(
        "Fake.br Corpus",
        "Fake.br-Corpus",
        "ISOT Dataset",
        "ISOT"
    );

    private static boolean isUntrustedPublisher(String publisher) {
        if (publisher == null) return false;
        String cleaned = publisher.trim();
        return UNTRUSTED_PUBLISHERS.contains(cleaned);
    }

    /**
     * Returns true if a publisher is not suitable for claim-level training data.
     * Used by external cleanup logic.
     */
    public static boolean isUntrusted(String publisher) {
        return isUntrustedPublisher(publisher);
    }

    /**
     * Cleans up the source/fonte field for storage as publisher.
     */
    private String cleanSource(String fonte) {
        if (fonte == null || fonte.isBlank()) return "Desconhecido";

        String cleaned = fonte.trim();

        // Normalize known sources
        return switch (cleaned) {
            case "Fake.br-Corpus" -> "Fake.br Corpus";
            case "ISOT Dataset" -> "ISOT Dataset";
            default -> cleaned;
        };
    }

    /**
     * Extracts keywords from the text content.
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

        return found.isEmpty()
            ? "factcheck, brasil"
            : String.join(", ", found);
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
        int cut = text.lastIndexOf(' ', maxLen);
        return text.substring(0, cut > 0 ? cut : maxLen);
    }
}
