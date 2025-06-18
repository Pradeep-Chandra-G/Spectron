package com.ragapp.controller;

import com.ragapp.entity.ChatMessage;
import com.ragapp.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }

        String answer = chatService.chat(question.trim());
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory() {
        List<ChatMessage> history = chatService.getChatHistory();
        return ResponseEntity.ok(history);
    }
}