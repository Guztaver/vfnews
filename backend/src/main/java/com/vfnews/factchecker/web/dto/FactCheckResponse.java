package com.vfnews.factchecker.web.dto;

import com.vfnews.factchecker.domain.FactCheck;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FactCheckResponse {
    private Long id;
    private String claim;
    private String result;
    private String source;
    private String rating;
    private String publisher;
    private String url;
    private LocalDateTime createdAt;

    public static FactCheckResponse fromEntity(FactCheck entity) {
        FactCheckResponse dto = new FactCheckResponse();
        dto.setId(entity.getId());
        dto.setClaim(entity.getClaim());
        dto.setResult(entity.getResult());
        dto.setSource(entity.getSource().name());
        dto.setRating(entity.getRating());
        dto.setPublisher(entity.getPublisher());
        dto.setUrl(entity.getUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
