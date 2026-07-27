package com.tuan.chatserver.service;

import com.tuan.chatserver.document.Message;
import com.tuan.chatserver.dto.MessageDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.MessageStatus;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.MessageMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Autowired
    public MessageService(MessageRepository messageRepository, ChatBoxRepository chatBoxRepository, UserRepository userRepository, MongoTransactionManager mongoTransactionManager, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.chatBoxRepository = chatBoxRepository;
        this.userRepository = userRepository;
        this.mongoTransactionManager = mongoTransactionManager;
        this.messageMapper = messageMapper;
    }

    public List<MessageDTO> findByChatBoxIdOrderByTimestampDesc(Long chatBoxId) {
        logger.debug("Fetching messages ordered by timestamp desc, chatBoxId={}", chatBoxId);
        List<Message> messages = messageRepository.findByChatBoxIdOrderByTimestampDesc(chatBoxId);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        logger.debug("Found {} message(s) for chatBoxId={}", messageDTOs.size(), chatBoxId);
        return messageDTOs;
    }

    public List<MessageDTO> findBySenderIdAndChatBoxIdOrderByTimestampDesc(Long senderId, Long chatBoxId) {
        logger.debug("Fetching messages by senderId={} and chatBoxId={}, ordered by timestamp desc", senderId, chatBoxId);
        List<Message> messages = messageRepository.findBySenderIdAndChatBoxIdOrderByTimestampDesc(senderId, chatBoxId);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        logger.debug("Found {} message(s) for senderId={}, chatBoxId={}", messageDTOs.size(), senderId, chatBoxId);
        return messageDTOs;
    }

    public List<MessageDTO> findByChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime){
        logger.debug("Fetching messages for chatBoxId={} between startTime={} and endTime={}", chatBoxId, startTime, endTime);
        List<Message> messages = messageRepository.findByChatBoxIdAndTimestampBetweenOrderByTimestampDesc(chatBoxId,startTime,endTime);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        logger.debug("Found {} message(s) for chatBoxId={} in given time range", messageDTOs.size(), chatBoxId);
        return messageDTOs;
    }

    public List<MessageDTO> findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long senderId, Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime){
        logger.debug("Fetching messages by senderId={} for chatBoxId={} between startTime={} and endTime={}", senderId, chatBoxId, startTime, endTime);
        List<Message> messages = messageRepository.findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampDesc(senderId,chatBoxId,startTime,endTime);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        logger.debug("Found {} message(s) for senderId={}, chatBoxId={} in given time range", messageDTOs.size(), senderId, chatBoxId);
        return messageDTOs;
    }

    @Transactional
    public void sendMessage(Long senderId, Long chatBoxId, String content){
        logger.info("Attempting to send message, senderId={}, chatBoxId={}", senderId, chatBoxId);
        ChatBox chatBox= chatBoxRepository.findById(chatBoxId).orElseThrow(() -> {
            logger.warn("Send message failed: chatbox not found, chatBoxId={}", chatBoxId);
            return new ChatBoxNotFoundException(chatBoxId);
        });
        User sender=userRepository.findById(senderId).orElseThrow(() -> {
            logger.warn("Send message failed: sender not found, senderId={}", senderId);
            return new UserNotFoundException(senderId);
        });
        if(chatBox.getUsers().contains(sender)){
            if(!content.isEmpty()){
                chatBox.setLastActiveTime(LocalDateTime.now());
                chatBoxRepository.save(chatBox);
                Message message=new Message(senderId, chatBoxId, LocalDateTime.now(), true, MessageStatus.SENT, content);
                messageRepository.save(message);
                logger.info("Message sent successfully, senderId={}, chatBoxId={}", senderId, chatBoxId);
            }else{
                logger.warn("Send message failed: content is empty, senderId={}, chatBoxId={}", senderId, chatBoxId);
                throw new EmptyMessageContentException();
            }
        }else{
            logger.warn("Send message failed: sender is not a member of chatbox, senderId={}, chatBoxId={}", senderId, chatBoxId);
            throw new UserNotInChatBoxException(chatBoxId, senderId);
        }
    }

    public void updateMessageStatus(String messageId){
        logger.info("Attempting to update message status, messageId={}", messageId);
        Optional<Message> message=messageRepository.findById(messageId);
        if(message.isPresent()){
            Message actualMessage=message.get();
            MessageStatus messageStatus=actualMessage.getStatus();
            if(messageStatus == MessageStatus.SENT){
                messageStatus=MessageStatus.RECEIVED;
            }else if(messageStatus == MessageStatus.RECEIVED){
                messageStatus=MessageStatus.SEEN;
            }else if(messageStatus == MessageStatus.SEEN){
                logger.warn("Update message status failed: message already at SEEN status, messageId={}", messageId);
                throw new MessageAlreadySeenException(messageId);
            }
            actualMessage.setStatus(messageStatus);
            try{
                messageRepository.save(actualMessage);
                logger.info("Message status updated successfully, messageId={}, newStatus={}", messageId, messageStatus);
            }catch(Exception e){
                logger.error("Error occurred while updating message status, messageId={}", messageId, e);
                throw new DataAccessFailureException(e);
            }
        }else{
            logger.warn("Update message status failed: message not found, messageId={}", messageId);
            throw new MessageNotExistsException(messageId);
        }
    }

    public void recallMessage(String messageId){
        logger.info("Attempting to recall message, messageId={}", messageId);
        Optional<Message> message=messageRepository.findById(messageId);
        if(message.isPresent()){
            Message actualMessage=message.get();
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
}