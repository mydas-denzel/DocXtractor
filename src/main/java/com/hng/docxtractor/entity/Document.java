package com.hng.docxtractor.entity;

import com.hng.docxtractor.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
    @Id
    private UUID id;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.PENDING; // <= default value

    @Column(nullable = false)
    private boolean viewed = false;

    @CreationTimestamp
    private Instant createdAt;
}
