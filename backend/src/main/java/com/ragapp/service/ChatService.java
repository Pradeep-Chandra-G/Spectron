//package com.ragapp.service;
//
//import com.ragapp.entity.ChatMessage;
//import com.ragapp.repository.ChatMessageRepository;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.ollama.OllamaChatModel;
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
//    private OllamaChatModel ollamaChatModel;
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    @Autowired
//    private ChatMessageRepository chatMessageRepository;
//
//    private static final double DISTANCE_THRESHOLD = 0.3; // Tune as needed
//
//    private static final String PROMPT_WITH_CONTEXT = """
//        You are a helpful AI assistant. Use the following document context only if it's clearly relevant to the user's question.
//        If the context is not helpful, answer using your general knowledge or engage in a friendly conversation.
//
//        Document Context:
//        {context}
//        """;
//
//    private static final String PROMPT_NO_CONTEXT = """
//        You are a helpful AI assistant. There is no document context available for this question.
//        Please respond using your general knowledge or engage conversationally as needed.
//        """;
//
//    public String chat(String question) {
//        try {
//            // Search for relevant documents
//            List<Document> retrievedDocs = vectorStore.similaritySearch(
//                    SearchRequest.query(question).withTopK(5)
//            );
//
//            for (Document doc : retrievedDocs) {
//                System.out.println(doc.getMetadata());  // check if it contains "score" or "similarity"
//            }
//
//            // Filter documents by similarity (if supported by your vector store)
//            List<Document> relevantDocs = retrievedDocs.stream()
//                    .filter(doc -> {
//                        Object distanceObj = doc.getMetadata().get("distance");
//                        if (distanceObj instanceof Number) {
//                            double distance = ((Number) distanceObj).doubleValue();
//                            return distance <= DISTANCE_THRESHOLD; // Lower is better; tweak as needed
//                        }
//                        return true;  // keep doc if distance is missing
//                    })
//                    .collect(Collectors.toList());
//
//            String context = relevantDocs.stream()
//                    .map(Document::getContent)
//                    .collect(Collectors.joining("\n\n"));
//
//            ChatClient chatClient = ChatClient.builder(ollamaChatModel).build();
//
//            String prompt = relevantDocs.isEmpty()
//                    ? PROMPT_NO_CONTEXT
//                    : PROMPT_WITH_CONTEXT.replace("{context}", context);
//
//            String answer = chatClient.prompt()
//                    .system(prompt)
//                    .user(question)
//                    .call()
//                    .content();
//
//            // Save interaction
//            ChatMessage chatMessage = new ChatMessage(question, answer, context);
//            chatMessageRepository.save(chatMessage);
//
//            return answer;
//
//        } catch (Exception e) {
//            return "I encountered an error while processing your question: " + e.getMessage();
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
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private OpenAiChatModel chatModel; // This will use Groq API

    @Autowired
    private VectorStore vectorStore; // This will be Pinecone

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private static final double SIMILARITY_THRESHOLD = 0.7; // Adjust based on your needs

    private static final String PROMPT_WITH_CONTEXT = """
        You are a helpful AI assistant named Spectron. Use the following document context only if it's clearly relevant to the user's question.
        If the context is not helpful, answer using your general knowledge or engage in a friendly conversation.
        
        Be concise but comprehensive in your responses. If you're using the document context, mention that your answer is based on the provided documents.

        Document Context:
        {context}
        
        Question: {question}
        """;

    private static final String PROMPT_NO_CONTEXT = """
        You are a helpful AI assistant named Spectron. There is no document context available for this question.
        Please respond using your general knowledge or engage conversationally as needed.
        
        Be helpful, concise, and friendly in your response.
        
        Question: {question}
        """;

    public String chat(String question) {
        try {
            // Search for relevant documents in Pinecone
            List<Document> retrievedDocs = vectorStore.similaritySearch(
                    SearchRequest.query(question)
                            .withTopK(5)
                            .withThreshold(SIMILARITY_THRESHOLD)
            );

            // Debug: Log retrieved documents
            System.out.println("Retrieved " + retrievedDocs.size() + " documents");
            for (Document doc : retrievedDocs) {
                System.out.println("Document metadata: " + doc.getMetadata());
            }

            // Build context from retrieved documents
            String context = retrievedDocs.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));

            // Create chat client
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            // Choose prompt based on whether we have relevant context
            String prompt = retrievedDocs.isEmpty()
                    ? PROMPT_NO_CONTEXT.replace("{question}", question)
                    : PROMPT_WITH_CONTEXT.replace("{context}", context).replace("{question}", question);

            // Get response from Groq
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Save interaction to database
            ChatMessage chatMessage = new ChatMessage(question, answer, context);
            chatMessageRepository.save(chatMessage);

            return answer;

        } catch (Exception e) {
            System.err.println("Error in chat service: " + e.getMessage());
            e.printStackTrace();
            return "I encountered an error while processing your question. Please try again or contact support if the issue persists.";
        }
    }

    public List<ChatMessage> getChatHistory() {
        return chatMessageRepository.findTop20ByOrderByTimestampDesc();
    }
}