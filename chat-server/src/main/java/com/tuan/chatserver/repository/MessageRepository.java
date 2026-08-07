package com.tuan.chatserver.repository;

import com.tuan.chatserver.document.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface MessageRepository extends MongoRepository<Message, String>{
    @Query("{ 'chatBoxId': ?0 }")
    List<Message> findByChatBoxIdForFirstPage(Long chatBoxId, Pageable pageable);

    @Query("{ 'chatBoxId': ?0, " +
            "'$or': [ " +
            "  { 'timestamp': { '$lt': ?1 } }, " +
            "  { 'timestamp': ?1, 'id': { '$lt': ?2 } } " +
            "] }")
    List<Message> findByChatBoxIdForNextPage(Long chatBoxId, LocalDateTime timestamp, String cursorId, Pageable pageable);

    @Query("{ 'chatBoxId': ?0, 'timestamp': { '$gte': ?1, '$lte': ?2 } }")
    List<Message> findByChatBoxIdAndTimestampBetweenForFirstPage(Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    @Query("{ 'chatBoxId': ?0, " +
            "'timestamp': { '$gte': ?1, '$lte': ?2 }, " +
            "'$or': [ " +
            "  { 'timestamp': { '$lt': ?3 } }, " +
            "  { 'timestamp': ?3, 'id': { '$lt': ?4 } } " +
            "] }")
    List<Message> findByChatBoxIdAndTimestampBetweenForNextPage(Long chatBoxId,
                                                               LocalDateTime startTime,
                                                               LocalDateTime endTime,
                                                               LocalDateTime cursorTimestamp,
                                                               String cursorId,
                                                               Pageable pageable);

    @Query("{ 'senderId': ?0, 'chatBoxId': ?1, 'timestamp': { '$gte': ?2, '$lte': ?3 } }")
    List<Message> findBySenderIdAndChatBoxIdAndTimestampBetweenForFirstPage(Long senderId,
                                                                           Long chatBoxId,
                                                                           LocalDateTime startTime,
                                                                           LocalDateTime endTime,
                                                                           Pageable pageable);

    @Query("{ 'senderId': ?0, 'chatBoxId': ?1, " +
            "'timestamp': { '$gte': ?2, '$lte': ?3 }, " +
            "'$or': [ " +
            "  { 'timestamp': { '$lt': ?4 } }, " +
            "  { 'timestamp': ?4, 'id': { '$lt': ?5 } } " +
            "] }")
    List<Message> findBySenderIdAndChatBoxIdAndTimestampBetweenForNextPage(Long senderId,
                                                                          Long chatBoxId,
                                                                          LocalDateTime startTime,
                                                                          LocalDateTime endTime,
                                                                          LocalDateTime cursorTimestamp,
                                                                          String cursorId,
                                                                          Pageable pageable);

    Long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    Set<Long> findDistinctChatBoxIdByChatBoxIdIn(List<Long> chatBoxIds);
}
