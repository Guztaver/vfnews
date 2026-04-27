package com.vfnews.factchecker.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactCheck {

    public enum Source {
        API, ML
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String claim;

    private String result;

    @Enumerated(EnumType.STRING)
    private Source source;

    private String rating;

    private String publisher;

    private String url;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
