package com.ragapp.service;

import com.ragapp.entity.Document;
import com.ragapp.entity.ProcessingStatus;
import com.ragapp.repository.DocumentRepository;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private VectorStore vectorStore;

    private final String uploadDir = "uploads/";

    public Document uploadDocument(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String filename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(filename);

        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create document entity
        Document document = new Document(
                filename,
                originalFilename,
                file.getContentType(),
                file.getSize(),
                filePath.toString()
        );

        Document savedDocument = documentRepository.save(document);

        // Process document asynchronously
        processDocumentAsync(savedDocument.getId());

        return savedDocument;
    }

    @Async
    public void processDocumentAsync(Long documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) return;

        try {
            document.setStatus(ProcessingStatus.PROCESSING);
            documentRepository.save(document);

            // Read and parse document
            DocumentReader reader = new TikaDocumentReader(
                    new FileSystemResource(document.getFilePath())
            );
            List<org.springframework.ai.document.Document> documents = reader.get();

            // Split into chunks
            TokenTextSplitter splitter = new TokenTextSplitter(500, 100, 5, 10000, true);
            List<org.springframework.ai.document.Document> chunks = splitter.apply(documents);

            // Add metadata
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("filename", document.getOriginalName());
                chunk.getMetadata().put("document_id", document.getId().toString());
            });

            // Store in vector database
            vectorStore.add(chunks);

            // Update document status
            document.setStatus(ProcessingStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

        } catch (Exception e) {
            document.setStatus(ProcessingStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

    public void deleteDocument(Long id) throws IOException {
        Document document = documentRepository.findById(id).orElse(null);
        if (document != null) {
            // Delete file
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            // Delete from database
            documentRepository.delete(document);
        }
    }
}
