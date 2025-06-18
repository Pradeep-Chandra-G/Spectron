package com.ragapp.repository;

import com.ragapp.entity.Document;
import com.ragapp.entity.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByStatusOrderByUploadedAtDesc(ProcessingStatus status);
    List<Document> findAllByOrderByUploadedAtDesc();
}
