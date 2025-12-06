package com.hng.docxtractor.repository;

import com.hng.docxtractor.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

}
