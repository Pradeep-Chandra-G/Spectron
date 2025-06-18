package com.ragapp.repository;

import com.ragapp.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByOrderByTimestampDesc();
    List<ChatMessage> findTop20ByOrderByTimestampDesc();
}