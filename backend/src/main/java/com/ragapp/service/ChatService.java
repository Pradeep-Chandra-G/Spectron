//package com.ragapp.service;
//
//import com.ragapp.entity.ChatMessage;
//import com.ragapp.repository.ChatMessageRepository;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.model.ChatResponse;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class ChatService {
//
//    @Autowired
//    private ChatClient.Builder chatClientBuilder;
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    @Autowired
//    private ChatMessageRepository chatMessageRepository;
//
//    private final String SYSTEM_PROMPT = """
//        You are a helpful AI assistant that answers questions based on the provided context from documents.
//        Use the following context to answer the user's question accurately and concisely.
//        If the context doesn't contain relevant information, politely say so.
//
//        Context:
//        {context}
//        """;
//
//    public String chat(String question) {
//        try {
//            // Search for relevant documents
//            List<Document> relevantDocs = vectorStore.similaritySearch(
//                    SearchRequest.query(question).withTopK(5)
//            );
//
//            // Prepare context
//            String context = relevantDocs.stream()
//                    .map(Document::getContent)
//                    .collect(Collectors.joining("\n\n"));
//
//            // Create chat client
//            ChatClient chatClient = chatClientBuilder.build();
//
//            // Get response from LLM
//            String answer = chatClient.prompt()
//                    .system(SYSTEM_PROMPT.replace("{context}", context))
//                    .user(question)
//                    .call()
//                    .content();
//
//            // Save chat message
//            ChatMessage chatMessage = new ChatMessage(question, answer, context);
//            chatMessageRepository.save(chatMessage);
//
//            return answer;
//
//        } catch (Exception e) {
//            return "I apologize, but I encountered an error while processing your question: " + e.getMessage();
//        }
//    }
//
//    public List<ChatMessage> getChatHistory() {
//        return chatMessageRepository.findTop20ByOrderByTimestampDesc();
//    }
//}


package com.ragapp.service;

import com.ragapp.entity.ChatMessage;
import com.ragapp.repository.ChatMessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private final String SYSTEM_PROMPT = """
        You are a helpful AI assistant that answers questions based on the provided context from documents.
        Use the following context to answer the user's question accurately and concisely.
        If the context doesn't contain relevant information, politely say so.
        
        Context:
        {context}
        """;

    public String chat(String question) {
        try {
            // Search for relevant documents
            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.query(question).withTopK(5)
            );

            // Prepare context
            String context = relevantDocs.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));

            // Create chat client with explicit Ollama model
            ChatClient chatClient = ChatClient.builder(ollamaChatModel).build();

            // Get response from LLM
            String answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT.replace("{context}", context))
                    .user(question)
                    .call()
                    .content();

            // Save chat message
            ChatMessage chatMessage = new ChatMessage(question, answer, context);
            chatMessageRepository.save(chatMessage);

            return answer;

        } catch (Exception e) {
            return "I apologize, but I encountered an error while processing your question: " + e.getMessage();
        }
    }

    public List<ChatMessage> getChatHistory() {
        return chatMessageRepository.findTop20ByOrderByTimestampDesc();
    }
}