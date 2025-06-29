package com.ragapp.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.PineconeVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${spring.ai.vectorstore.pinecone.environment}")
    private String pineconeEnvironment;

    @Value("${spring.ai.vectorstore.pinecone.project-id}")
    private String pineconeProjectId;

    @Value("${spring.ai.vectorstore.pinecone.index-name}")
    private String pineconeIndexName;

    @Value("${spring.ai.vectorstore.pinecone.namespace:default}")
    private String pineconeNamespace;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return PineconeVectorStore.builder()
                .apiKey(pineconeApiKey)
                .environment(pineconeEnvironment)
                .projectId(pineconeProjectId)
                .indexName(pineconeIndexName)
                .namespace(pineconeNamespace)
                .embeddingModel(embeddingModel)
                .build();
    }
}