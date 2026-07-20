package com.tuan.chatserver.repository;

import com.tuan.chatserver.document.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String>{
    List<Message> findByChatBoxIdOrderByTimestampDesc(Long chatBoxId);
    List<Message> findBySenderIdAndChatBoxIdOrderByTimestampDesc(Long senderId, Long chatBoxId);
    List<Message> findByChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime);
    List<Message> findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long senderId, Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime);
    Long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
}
