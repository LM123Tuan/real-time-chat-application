package com.tuan.chatserver.service;

import com.mongodb.client.result.UpdateResult;
import com.tuan.chatserver.document.Message;
import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.EventType;
import com.tuan.chatserver.enums.MessageStatus;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.MessageMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import com.tuan.chatserver.util.CursorCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MessageRepository messageRepository;
    private final ChatBoxRepository chatBoxRepository;
    private final UserRepository userRepository;
    private final MongoTransactionManager mongoTransactionManager;
    private final MessageMapper messageMapper;
    private final CursorCodec cursorCodec;
    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MessageService(MessageRepository messageRepository,
                          ChatBoxRepository chatBoxRepository,
                          UserRepository userRepository,
                          MongoTransactionManager mongoTransactionManager,
                          MessageMapper messageMapper,
                          CursorCodec cursorCodec,
                          MongoTemplate mongoTemplate,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.chatBoxRepository = chatBoxRepository;
        this.userRepository = userRepository;
        this.mongoTransactionManager = mongoTransactionManager;
        this.messageMapper = messageMapper;
        this.cursorCodec = cursorCodec;
        this.mongoTemplate = mongoTemplate;
        this.messagingTemplate=messagingTemplate;
    }

    @Transactional(readOnly = true)
    public CursorPaginationResponse<List<MessageDTO>> findMessages(
            Long requesterId, Long chatBoxId, SearchMessageRequest request,
            CursorPaginationRequest paginationRequest) {

        ChatBox chatBox = chatBoxRepository.findById(chatBoxId).orElseThrow(() -> {
            logger.warn("Validation failed: chatbox not found, chatBoxId={}", chatBoxId);
            return new ChatBoxNotFoundException(chatBoxId);
        });
        User requester = userRepository.findById(requesterId).orElseThrow(() -> {
            logger.warn("Validation failed: user not found, userId={}", requesterId);
            return new UserNotFoundException(requesterId);
        });
        Long senderId = request.getSenderId();

        if (!chatBox.getUsers().contains(requester)) {
            throw new UserNotInChatBoxException(chatBoxId, requesterId);
        }

        LocalDateTime startTime = request.getStartTime();
        if (request.getStartTime() == null) {
            startTime = chatBox.getCreateTime();
        }

        LocalDateTime endTime = request.getEndTime();
        if (request.getEndTime() == null) {
            endTime = chatBox.getLastActiveTime();
        }

        logger.debug("Resolving message query, chatBoxId={}, senderId={}, startTime={}, endTime={}",
                chatBoxId, senderId, request.getStartTime(), request.getEndTime());

        Pageable pageable = PageRequest.of(0, paginationRequest.getSize() + 1,
                Sort.by(Sort.Direction.DESC, "timestamp", "id"));

        PageCursor<String> cursorData = paginationRequest.getCursor() == null
                ? null
                : cursorCodec.decode(paginationRequest.getCursor(), new TypeReference<PageCursor<String>>() {});

        List<Message> messages;
        if (senderId != null) {
            User sender = userRepository.findById(senderId).orElseThrow(() -> {
                logger.warn("Validation failed: user not found, userId={}", senderId);
                return new UserNotFoundException(senderId);
            });

            if (!chatBox.getUsers().contains(sender)) {
                throw new UserNotInChatBoxException(chatBoxId, senderId);
            }

            if (cursorData == null) {
                messages = messageRepository.findBySenderIdAndChatBoxIdAndTimestampBetweenForFirstPage(
                        senderId, chatBoxId, startTime, endTime, pageable);
            } else {
                messages = messageRepository.findBySenderIdAndChatBoxIdAndTimestampBetweenForNextPage(
                        senderId, chatBoxId, startTime, endTime,
                        cursorData.getTimestamp(), cursorData.getId(),
                        pageable);
            }
        } else {
            if (cursorData == null) {
                messages = messageRepository.findByChatBoxIdAndTimestampBetweenForFirstPage(
                        chatBoxId, startTime, endTime, pageable);
            } else {
                messages = messageRepository.findByChatBoxIdAndTimestampBetweenForNextPage(
                        chatBoxId, startTime, endTime,
                        cursorData.getTimestamp(), cursorData.getId(),
                        pageable);
            }
        }

        boolean hasNext = messages.size() > paginationRequest.getSize();
        if (hasNext) {
            messages = messages.subList(0, paginationRequest.getSize());
        }

        List<MessageDTO> messageDTOs = mapToDTOList(messages);

        String nextCursor = null;
        if (!messages.isEmpty()) {
            Message lastMessage = messages.get(messages.size() - 1);
            PageCursor<String> nextCursorData = new PageCursor<>(lastMessage.getTimestamp(), lastMessage.getId());
            nextCursor = cursorCodec.encode(nextCursorData);
        }

        CursorPaginationResponse<List<MessageDTO>> response =
                new CursorPaginationResponse<>(messageDTOs, nextCursor, hasNext);

        logger.debug("Found {} message(s) for chatBoxId={}, senderId={}, startTime={}, endTime={}",
                messageDTOs.size(), chatBoxId, senderId, request.getStartTime(), request.getEndTime());
        return response;
    }

    private List<MessageDTO> mapToDTOList(List<Message> messages) {
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            messageDTOs.add(messageMapper.mapMessageToMessageDTO(message));
        }
        return messageDTOs;
    }

    @Transactional
    public MessageDTO sendMessage(Long senderId, Long chatBoxId, String content){
        logger.info("Attempting to send message, senderId={}, chatBoxId={}", senderId, chatBoxId);
        validateUserInChatBox(senderId, chatBoxId);

        if(content.isEmpty()){
            logger.warn("Send message failed: content is empty, senderId={}, chatBoxId={}", senderId, chatBoxId);
            throw new EmptyMessageContentException();
        }

        ChatBox chatBox = chatBoxRepository.findById(chatBoxId).orElseThrow(() -> {
            logger.warn("Send message failed: chatbox not found, chatBoxId={}", chatBoxId);
            return new ChatBoxNotFoundException(chatBoxId);
        });
        chatBox.setLastActiveTime(LocalDateTime.now());
        chatBoxRepository.save(chatBox);
        Message message = new Message(senderId, chatBoxId, LocalDateTime.now(), true, MessageStatus.SENT, content);
        messageRepository.save(message);
        logger.info("Message sent successfully, senderId={}, chatBoxId={}", senderId, chatBoxId);
        MessageDTO dto = messageMapper.mapMessageToMessageDTO(message);
        return dto;
    }

    public void markMessageAsReceived(Long requesterId, String messageId) {
        logger.info("Attempting to mark message as RECEIVED, requesterId={}, messageId={}", requesterId, messageId);
        Message actualMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    logger.warn("Mark message as RECEIVED failed: message not found, messageId={}", messageId);
                    return new MessageNotExistsException(messageId);
                });

        validateUserInChatBox(requesterId, actualMessage.getChatBoxId());

        if (actualMessage.getStatus() != MessageStatus.SENT) {
            logger.warn("Mark message as RECEIVED failed: message not at SENT status, messageId={}, currentStatus={}",
                    messageId, actualMessage.getStatus());
            throw new InvalidMessageStatusException(messageId, actualMessage.getStatus());
        }

        actualMessage.setStatus(MessageStatus.RECEIVED);
        try {
            messageRepository.save(actualMessage);
            logger.info("Message marked as RECEIVED successfully, requesterId={}, messageId={}", requesterId, messageId);
        } catch (Exception e) {
            logger.error("Error occurred while marking message as RECEIVED, messageId={}", messageId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void markMessageAsSeen(Long requesterId, String messageId) {
        logger.info("Attempting to mark message as SEEN, requesterId={}, messageId={}", requesterId, messageId);
        Message actualMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    logger.warn("Mark message as SEEN failed: message not found, messageId={}", messageId);
                    return new MessageNotExistsException(messageId);
                });

        validateUserInChatBox(requesterId, actualMessage.getChatBoxId());

        if (actualMessage.getStatus() == MessageStatus.SEEN) {
            logger.warn("Mark message as SEEN failed: message already at SEEN status, messageId={}", messageId);
            throw new InvalidMessageStatusException(messageId);
        }

        actualMessage.setStatus(MessageStatus.SEEN);
        try {
            messageRepository.save(actualMessage);
            logger.info("Message marked as SEEN successfully, requesterId={}, messageId={}", requesterId, messageId);
        } catch (Exception e) {
            logger.error("Error occurred while marking message as SEEN, messageId={}", messageId, e);
            throw new DataAccessFailureException(e);
        }
    }

    void markSentMessagesAsReceivedForChatBoxes(Long requesterId, List<ChatBox> chatBoxes) {
        if (chatBoxes.isEmpty()) {
            return;
        }

        List<Long> chatBoxIds = chatBoxes.stream()
                .map(ChatBox::getId)
                .toList();

        logger.debug("Marking SENT messages as RECEIVED, requesterId={}, chatBoxCount={}",
                requesterId, chatBoxIds.size());

        Query query = new Query(Criteria.where("chatBoxId").in(chatBoxIds)
                .and("status").is(MessageStatus.SENT)
                .and("senderId").ne(requesterId));
        Update update = new Update().set("status", MessageStatus.RECEIVED);

        try {
            UpdateResult result = mongoTemplate.updateMulti(query, update, Message.class);
            logger.info("Marked {} message(s) as RECEIVED, requesterId={}", result.getModifiedCount(), requesterId);
        } catch (Exception e) {
            logger.error("Error occurred while marking messages as RECEIVED, requesterId={}", requesterId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public void recallMessage(Long requesterId, String messageId){
        logger.info("Attempting to recall message, requesterId={}, messageId={}", requesterId, messageId);
        Optional<Message> message=messageRepository.findById(messageId);
        if(message.isPresent()){
            Message actualMessage=message.get();
            validateUserInChatBox(requesterId, actualMessage.getChatBoxId());

            if(!actualMessage.getSenderId().equals(requesterId)){
                throw new UserIsNotMessageSenderException(requesterId, messageId);
            }
            boolean messageViewable=actualMessage.isViewable();
            if(messageViewable){
                actualMessage.setViewable(false);
                try{
                    messageRepository.save(actualMessage);
                    logger.info("Message recalled successfully, messageId={}", messageId);
                }catch(Exception e){
                    logger.error("Error occurred while recalling message, messageId={}", messageId, e);
                    throw new DataAccessFailureException(e);
                }
            }else{
                logger.warn("Recall message failed: message already recalled (not viewable), messageId={}", messageId);
                throw new MessageAlreadyRecallException(messageId);
            }
        }else{
            logger.warn("Recall message failed: message not found, messageId={}", messageId);
            throw new MessageNotExistsException(messageId);
        }
    }

    private void validateUserInChatBox(Long userId, Long chatBoxId) {
        ChatBox chatBox = chatBoxRepository.findById(chatBoxId).orElseThrow(() -> {
            logger.warn("Validation failed: chatbox not found, chatBoxId={}", chatBoxId);
            return new ChatBoxNotFoundException(chatBoxId);
        });
        User user = userRepository.findById(userId).orElseThrow(() -> {
            logger.warn("Validation failed: user not found, userId={}", userId);
            return new UserNotFoundException(userId);
        });
        if (!chatBox.getUsers().contains(user)) {
            logger.warn("Validation failed: user is not a member of chatbox, userId={}, chatBoxId={}", userId, chatBoxId);
            throw new UserNotInChatBoxException(chatBoxId, userId);
        }
    }

    public CursorPaginationResponse<List<MessageDTO>> loadAllMessagesForChatBox(
            Long userId, Long chatBoxId, CursorPaginationRequest request) {

        logger.debug("Loading messages for chatBoxId={}, userId={}", chatBoxId, userId);

        validateUserInChatBox(userId, chatBoxId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1,
                Sort.by(Sort.Direction.DESC, "timestamp", "id"));

        List<Message> messages;
        if (request.getCursor() == null) {
            messages = messageRepository.findByChatBoxIdForFirstPage(chatBoxId, pageable);
        } else {
            PageCursor<String> cursorData = cursorCodec.decode(
                    request.getCursor(), new TypeReference<PageCursor<String>>() {});
            messages = messageRepository.findByChatBoxIdForNextPage(
                    chatBoxId,
                    cursorData.getTimestamp(),
                    cursorData.getId(),
                    pageable
            );
        }

        boolean hasNext = messages.size() > request.getSize();
        if (hasNext) {
            messages = messages.subList(0, request.getSize());
        }

        markReceivedMessagesAsSeen(userId, chatBoxId, messages);

        messagingTemplate.convertAndSend("/topic/chatbox/" + chatBoxId,
                new ChatEvent<>(EventType.MESSAGE_STATUS_UPDATED_TO_SEEN, null));

        List<MessageDTO> dtos = mapToDTOList(messages);

        String nextCursor = null;
        if (!messages.isEmpty()) {
            Message lastMessage = messages.get(messages.size() - 1);
            PageCursor<String> cursorData = new PageCursor<>(lastMessage.getTimestamp(), lastMessage.getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<MessageDTO>> response =
                new CursorPaginationResponse<>(dtos, nextCursor, hasNext);

        logger.debug("Found {} message(s) for chatBoxId={}", dtos.size(), chatBoxId);
        return response;
    }

    private void markReceivedMessagesAsSeen(Long requesterId, Long chatBoxId, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        logger.debug("Marking messages as SEEN, requesterId={}, chatBoxId={}, messageCount={}",
                requesterId, chatBoxId, messages.size());

        validateUserInChatBox(requesterId, chatBoxId);

        for (Message message : messages) {
            if (message.getStatus() != MessageStatus.SEEN && !message.getSenderId().equals(requesterId)) {
                message.setStatus(MessageStatus.SEEN);
            }
        }

        try {
            messageRepository.saveAll(messages);
            logger.info("Marked message(s) as SEEN, requesterId={}, chatBoxId={}", requesterId, chatBoxId);
        } catch (Exception e) {
            logger.error("Error occurred while marking messages as SEEN, requesterId={}, chatBoxId={}",
                    requesterId, chatBoxId, e);
            throw new DataAccessFailureException(e);
        }
    }
}