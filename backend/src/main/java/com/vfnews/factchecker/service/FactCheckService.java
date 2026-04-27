package com.vfnews.factchecker.service;

import com.vfnews.factchecker.domain.FactCheck;
import com.vfnews.factchecker.repository.FactCheckRepository;
import com.vfnews.factchecker.service.google.GoogleFactCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FactCheckService {

    private final FactCheckRepository repository;
    private final GoogleFactCheckService googleService;
    private final MLService mlService;

    public FactCheck checkClaim(String claimText) {
        // 1. Try Google Fact Check API
        Map<String, String> googleResult = googleService.checkClaim(claimText);
        if (googleResult != null) {
            return repository.save(FactCheck.builder()
                    .claim(claimText)
                    .result(googleResult.get("rating"))
                    .source(FactCheck.Source.API)
                    .rating(googleResult.get("rating"))
                    .publisher(googleResult.get("publisher"))
                    .url(googleResult.get("url"))
                    .build());
        }

        // 2. Try Local ML Model
        Map<String, String> mlResult = mlService.predict(claimText);
        if (mlResult != null) {
            return repository.save(FactCheck.builder()
                    .claim(claimText)
                    .result(mlResult.get("rating"))
                    .source(FactCheck.Source.ML)
                    .rating(mlResult.get("rating"))
                    .build());
        }

        return null;
    }
}
