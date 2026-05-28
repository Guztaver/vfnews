package com.vfnews.factchecker.web;

import com.vfnews.factchecker.service.MLService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MLMetricsController {

    private final MLService mlService;

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        Map<String, Object> metrics = mlService.getMetrics();
        if (metrics == null || metrics.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Model has not been trained yet. No metrics available."
            ));
        }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        Map<String, Object> metrics = mlService.getMetrics();
        Map<String, Object> info = new LinkedHashMap<>();

        if (metrics != null && !metrics.isEmpty()) {
            info.put("algorithm", metrics.getOrDefault("algorithm", "Multinomial Naive Bayes"));
            info.put("datasetSize", metrics.getOrDefault("datasetSize", 0));
            info.put("vocabularySize", metrics.getOrDefault("vocabularySize", 0));
        } else {
            info.put("algorithm", "Multinomial Naive Bayes");
            info.put("datasetSize", 0);
            info.put("vocabularySize", 0);
            info.put("message", "Model has not been trained yet.");
        }
        return ResponseEntity.ok(info);
    }
}
