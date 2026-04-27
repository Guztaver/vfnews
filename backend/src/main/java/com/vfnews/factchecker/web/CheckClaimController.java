package com.vfnews.factchecker.web;

import com.vfnews.factchecker.domain.FactCheck;
import com.vfnews.factchecker.service.FactCheckService;
import com.vfnews.factchecker.web.dto.CheckClaimRequest;
import com.vfnews.factchecker.web.dto.FactCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CheckClaimController {

    private final FactCheckService factCheckService;

    @PostMapping("/check/")
    public ResponseEntity<?> checkClaim(@RequestBody CheckClaimRequest request) {
        if (request.getClaim() == null || request.getClaim().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Claim text is required."));
        }

        FactCheck result = factCheckService.checkClaim(request.getClaim());
        if (result != null) {
            return ResponseEntity.ok(FactCheckResponse.fromEntity(result));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No fact check found for this claim."));
    }
}
