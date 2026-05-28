package com.vfnews.factchecker.service.google;

import java.util.List;
import lombok.Data;

@Data
public class GoogleFactCheckResponse {

    private List<Claim> claims;
    private String nextPageToken;

    @Data
    public static class Claim {

        private String text;
        private String claimant;
        private List<ClaimReview> claimReview;
    }

    @Data
    public static class ClaimReview {

        private Publisher publisher;
        private String url;
        private String textualRating;
        private String languageCode;
    }

    @Data
    public static class Publisher {

        private String name;
        private String site;
    }
}
