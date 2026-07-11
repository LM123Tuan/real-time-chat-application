package com.tuan.chatserver.repository;

import com.tuan.chatserver.document.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String>{
    List<Message> findByChatBoxIdOrderByTimestampAsc(Long chatBoxId);
    List<Message> findBySenderIdAndChatBoxIdOrderByTimestampAsc(Long senderId, Long chatBoxId);
    List<Message> findByChatBoxIdAndTimestampBetweenOrderByTimestampAsc(Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime);
    List<Message> findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampAsc(Long senderId, Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime);
}
