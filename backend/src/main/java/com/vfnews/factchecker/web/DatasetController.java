package com.vfnews.factchecker.web;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.service.MLService;
import com.vfnews.factchecker.web.dto.DatasetEntryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DatasetController {

    private final DatasetEntryRepository repository;
    private final MLService mlService;

    @GetMapping
    public ResponseEntity<List<DatasetEntry>> getAllEntries() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<DatasetEntry> addEntry(@Valid @RequestBody DatasetEntryRequest request) {
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
            return ResponseEntity.ok(Map.of("message", "Model training completed successfully."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error training model: " + e.getMessage()));
        }
    }
}
