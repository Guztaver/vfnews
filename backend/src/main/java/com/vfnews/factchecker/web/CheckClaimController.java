package com.vfnews.factchecker.web;

import com.vfnews.factchecker.domain.FactCheck;
import com.vfnews.factchecker.service.FactCheckService;
import com.vfnews.factchecker.web.dto.CheckClaimRequest;
import com.vfnews.factchecker.web.dto.FactCheckResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CheckClaimController {

    private final FactCheckService factCheckService;

    @PostMapping("/check/")
    public ResponseEntity<?> checkClaim(
        @RequestBody CheckClaimRequest request
    ) {
        if (request.getClaim() == null || request.getClaim().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Claim text is required.")
            );
        }

        FactCheckService.CheckResult result = factCheckService.checkClaim(
            request.getClaim()
        );
        if (result == null || result.factCheck() == null) {
            return ResponseEntity.ok(
                Map.of(
                    "message",
                    "Não foi possível verificar esta alegação no momento. Tente novamente."
                )
            );
        }

        FactCheckResponse dto = FactCheckResponse.fromEntity(
            result.factCheck()
        );
        if (result.matchedText() != null) {
            dto.setMatchedClaim(result.matchedText());
        }

        return ResponseEntity.ok(dto);
    }
}
