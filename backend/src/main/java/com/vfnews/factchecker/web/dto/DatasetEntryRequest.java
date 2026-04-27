package com.vfnews.factchecker.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DatasetEntryRequest {
    @NotBlank(message = "Text is required")
    private String text;

    @NotBlank(message = "Label is required")
    private String label;

    private String keywords;
}
