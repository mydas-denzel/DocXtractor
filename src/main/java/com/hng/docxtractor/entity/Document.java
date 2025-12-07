package com.hng.docxtractor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;
    private String storagePath;
    private String contentType;
    private Long sizeBytes;

    @Lob
    private String extractedText;

    private boolean containsImages;

    private int imageCount;

    private boolean analyzed;

    private String documentType; // invoice, cv, report, etc.

    @Lob
    private String summary;

    /**
     * JSON string containing extracted structured metadata (names, dates, amounts, emails, phones)
     */
    @Lob
    private String metadataJson;

    @CreationTimestamp
    private Instant createdAt;
}
