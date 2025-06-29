//package com.ragapp.service;
//
//import com.ragapp.entity.Document;
//import com.ragapp.entity.ProcessingStatus;
//import com.ragapp.repository.DocumentRepository;
//import org.springframework.ai.document.DocumentReader;
//import org.springframework.ai.reader.tika.TikaDocumentReader;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//public class DocumentService {
//
//    @Autowired
//    private DocumentRepository documentRepository;
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    private final String uploadDir = "uploads/";
//
//    public Document uploadDocument(MultipartFile file) throws IOException {
//        // Create upload directory if it doesn't exist
//        Path uploadPath = Paths.get(uploadDir);
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//        }
//
//        // Generate unique filename
//        String originalFilename = file.getOriginalFilename();
//        String filename = UUID.randomUUID().toString() + "_" + originalFilename;
//        Path filePath = uploadPath.resolve(filename);
//
//        // Save file
//        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//        // Create document entity
//        Document document = new Document(
//                filename,
//                originalFilename,
//                file.getContentType(),
//                file.getSize(),
//                filePath.toString()
//        );
//
//        Document savedDocument = documentRepository.save(document);
//
//        // Process document asynchronously
//        processDocumentAsync(savedDocument.getId());
//
//        return savedDocument;
//    }
//
//    @Async
//    public void processDocumentAsync(Long documentId) {
//        Document document = documentRepository.findById(documentId).orElse(null);
//        if (document == null) return;
//
//        try {
//            document.setStatus(ProcessingStatus.PROCESSING);
//            documentRepository.save(document);
//
//            // Read and parse document
//            DocumentReader reader = new TikaDocumentReader(
//                    new FileSystemResource(document.getFilePath())
//            );
//            List<org.springframework.ai.document.Document> documents = reader.get();
//
//            // Split into chunks
//            TokenTextSplitter splitter = new TokenTextSplitter(500, 100, 5, 10000, true);
//            List<org.springframework.ai.document.Document> chunks = splitter.apply(documents);
//
//            // Add metadata
//            chunks.forEach(chunk -> {
//                chunk.getMetadata().put("filename", document.getOriginalName());
//                chunk.getMetadata().put("document_id", document.getId().toString());
//            });
//
//            // Store in vector database
//            vectorStore.add(chunks);
//
//            // Update document status
//            document.setStatus(ProcessingStatus.COMPLETED);
//            document.setChunkCount(chunks.size());
//            documentRepository.save(document);
//
//        } catch (Exception e) {
//            document.setStatus(ProcessingStatus.FAILED);
//            document.setErrorMessage(e.getMessage());
//            documentRepository.save(document);
//        }
//    }
//
//    public List<Document> getAllDocuments() {
//        return documentRepository.findAllByOrderByUploadedAtDesc();
//    }
//
//    public Document getDocumentById(Long id) {
//        return documentRepository.findById(id).orElse(null);
//    }
//
//    public void deleteDocument(Long id) throws IOException {
//        Document document = documentRepository.findById(id).orElse(null);
//        if (document != null) {
//            // Delete file
//            Path filePath = Paths.get(document.getFilePath());
//            if (Files.exists(filePath)) {
//                Files.delete(filePath);
//            }
//
//            // Delete from database
//            documentRepository.delete(document);
//        }
//    }
//}

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
    private VectorStore vectorStore; // This will be Pinecone

    private final String uploadDir = "uploads/";

    public Document uploadDocument(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidFileType(originalFilename)) {
            throw new IOException("Unsupported file type. Please upload PDF, DOC, DOCX, TXT, or MD files.");
        }

        // Generate unique filename
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
        if (document == null) {
            System.err.println("Document not found with ID: " + documentId);
            return;
        }

        try {
            System.out.println("Starting processing for document: " + document.getOriginalName());

            document.setStatus(ProcessingStatus.PROCESSING);
            documentRepository.save(document);

            // Read and parse document using Tika
            DocumentReader reader = new TikaDocumentReader(
                    new FileSystemResource(document.getFilePath())
            );
            List<org.springframework.ai.document.Document> documents = reader.get();

            if (documents.isEmpty()) {
                throw new RuntimeException("No content could be extracted from the document");
            }

            // Split into chunks with optimized parameters
            TokenTextSplitter splitter = new TokenTextSplitter(
                    600,  // chunk size
                    100,  // overlap
                    5,    // min chunk size
                    10000, // max chunk size
                    true  // keep separator
            );
            List<org.springframework.ai.document.Document> chunks = splitter.apply(documents);

            // Add comprehensive metadata to each chunk
            for (int i = 0; i < chunks.size(); i++) {
                org.springframework.ai.document.Document chunk = chunks.get(i);
                chunk.getMetadata().put("filename", document.getOriginalName());
                chunk.getMetadata().put("document_id", document.getId().toString());
                chunk.getMetadata().put("chunk_index", String.valueOf(i));
                chunk.getMetadata().put("total_chunks", String.valueOf(chunks.size()));
                chunk.getMetadata().put("file_type", document.getContentType());
                chunk.getMetadata().put("upload_date", document.getUploadedAt().toString());

                // Add a unique ID for each chunk in Pinecone
                chunk.getMetadata().put("chunk_id", document.getId() + "_chunk_" + i);
            }

            System.out.println("Created " + chunks.size() + " chunks for document: " + document.getOriginalName());

            // Store in Pinecone vector database
            vectorStore.add(chunks);

            // Update document status
            document.setStatus(ProcessingStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

            System.out.println("Successfully processed document: " + document.getOriginalName());

        } catch (Exception e) {
            System.err.println("Error processing document " + document.getOriginalName() + ": " + e.getMessage());
            e.printStackTrace();

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
            try {
                // Delete chunks from Pinecone
                // Note: This is a simplified approach. In production, you might want to
                // implement a more sophisticated deletion strategy
                deleteDocumentChunksFromPinecone(document);

                // Delete physical file
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }

                // Delete from database
                documentRepository.delete(document);

                System.out.println("Successfully deleted document: " + document.getOriginalName());

            } catch (Exception e) {
                System.err.println("Error deleting document: " + e.getMessage());
                throw new IOException("Failed to delete document: " + e.getMessage());
            }
        }
    }

    private void deleteDocumentChunksFromPinecone(Document document) {
        // This is a placeholder for Pinecone deletion logic
        // You would need to implement this based on your Pinecone client
        // For now, we'll just log the action
        System.out.println("Would delete chunks for document ID: " + document.getId() + " from Pinecone");

        // If you have access to Pinecone client directly, you could delete by filter:
        // pineconeClient.delete(Filter.eq("document_id", document.getId().toString()));
    }

    private boolean isValidFileType(String filename) {
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".pdf") ||
                lowerFilename.endsWith(".doc") ||
                lowerFilename.endsWith(".docx") ||
                lowerFilename.endsWith(".txt") ||
                lowerFilename.endsWith(".md");
    }
}