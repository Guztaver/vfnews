package com.vfnews.factchecker.web;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.service.MLService;
import com.vfnews.factchecker.web.dto.DatasetEntryRequest;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DatasetController {

    private final DatasetEntryRepository repository;
    private final MLService mlService;

    /** List entries with pagination. Use ?page=0&size=20 */
    @GetMapping
    public ResponseEntity<List<DatasetEntry>> getAllEntries(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
            repository.findAll(PageRequest.of(page, size)).getContent()
        );
    }

    /** Stats: counts by label, keyword, publisher, plus latest entries. */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<DatasetEntry> all = repository.findAll();
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("total", all.size());

        // By label
        Map<String, Long> byLabel = all
            .stream()
            .collect(
                Collectors.groupingBy(
                    DatasetEntry::getLabel,
                    Collectors.counting()
                )
            );
        stats.put("byLabel", byLabel);

        // By keyword (top 15)
        Map<String, Long> byKeyword = all
            .stream()
            .filter(e -> e.getKeywords() != null)
            .flatMap(e -> Arrays.stream(e.getKeywords().split(",\\s*")))
            .collect(Collectors.groupingBy(k -> k, Collectors.counting()));
        stats.put(
            "byKeyword",
            byKeyword
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                    )
                )
        );

        // By publisher (top 10)
        Map<String, Long> byPublisher = all
            .stream()
            .filter(
                e -> e.getPublisher() != null && !e.getPublisher().isBlank()
            )
            .collect(
                Collectors.groupingBy(
                    DatasetEntry::getPublisher,
                    Collectors.counting()
                )
            );
        stats.put(
            "byPublisher",
            byPublisher
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                    )
                )
        );

        // Latest 10 entries
        List<Map<String, String>> latest = all
            .stream()
            .sorted(Comparator.comparing(DatasetEntry::getCreatedAt).reversed())
            .limit(10)
            .map(e ->
                Map.of(
                    "text",
                    e.getText(),
                    "label",
                    e.getLabel(),
                    "keywords",
                    e.getKeywords() != null ? e.getKeywords() : "",
                    "publisher",
                    e.getPublisher() != null ? e.getPublisher() : ""
                )
            )
            .toList();
        stats.put("latest", latest);

        return ResponseEntity.ok(stats);
    }

    @PostMapping
    public ResponseEntity<DatasetEntry> addEntry(
        @Valid @RequestBody DatasetEntryRequest request
    ) {
        DatasetEntry entry = DatasetEntry.builder()
            .text(request.getText())
            .label(request.getLabel())
            .keywords(request.getKeywords())
            .build();
        return ResponseEntity.ok(repository.save(entry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/train")
    public ResponseEntity<?> trainModel() {
        try {
            mlService.train();
            return ResponseEntity.ok(
                Map.of("message", "Model training completed successfully.")
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Error training model: " + e.getMessage())
            );
        }
    }
}
