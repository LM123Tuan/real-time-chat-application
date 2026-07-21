package com.tuan.chatserver.service;

import com.tuan.chatserver.document.Message;
import com.tuan.chatserver.dto.MessageDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.MessageStatus;
import com.tuan.chatserver.mapper.MessageMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class MessageService {
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
        List<Message> messages = messageRepository.findByChatBoxIdOrderByTimestampDesc(chatBoxId);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    public List<MessageDTO> findBySenderIdAndChatBoxIdOrderByTimestampDesc(Long senderId, Long chatBoxId) {
        List<Message> messages = messageRepository.findBySenderIdAndChatBoxIdOrderByTimestampDesc(senderId, chatBoxId);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    public List<MessageDTO> findByChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime){
        List<Message> messages = messageRepository.findByChatBoxIdAndTimestampBetweenOrderByTimestampDesc(chatBoxId,startTime,endTime);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    public List<MessageDTO> findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long senderId, Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime){
        List<Message> messages = messageRepository.findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampDesc(senderId,chatBoxId,startTime,endTime);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    @Transactional
    public boolean sendMessage(Long senderId, Long chatBoxId, String content){
        ChatBox chatBox= chatBoxRepository.findById(chatBoxId).orElseThrow(() -> new NoSuchElementException("Cannot find chatbox"));
        User sender=userRepository.findById(senderId).orElseThrow(() -> new NoSuchElementException("Cannot find sender"));
        if(chatBox.getUsers().contains(sender)){
            if(!content.isEmpty()){
                chatBox.setLastActiveTime(LocalDateTime.now());
                chatBoxRepository.save(chatBox);
                Message message=new Message(senderId, chatBoxId, LocalDateTime.now(), true, MessageStatus.SENT, content);
                messageRepository.save(message);
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    public boolean updateMessageStatus(String messageId){
        Optional<Message> message=messageRepository.findById(messageId);
        if(message.isPresent()){
            Message actualMessage=message.get();
            MessageStatus messageStatus=actualMessage.getStatus();
            if(messageStatus == MessageStatus.SENT){
                messageStatus=MessageStatus.RECEIVED;
            }else if(messageStatus == MessageStatus.RECEIVED){
                messageStatus=MessageStatus.SEEN;
            }else if(messageStatus == MessageStatus.SEEN){
                return false;
            }
            actualMessage.setStatus(messageStatus);
            try{
                messageRepository.save(actualMessage);
                return true;
            }catch(Exception e){
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
        }
    }

    public boolean recallMessage(String messageId){
        Optional<Message> message=messageRepository.findById(messageId);
        if(message.isPresent()){
            Message actualMessage=message.get();
            boolean messageViewable=actualMessage.isViewable();
            if(messageViewable){
                actualMessage.setViewable(false);
                try{
                    messageRepository.save(actualMessage);
                    return true;
                }catch(Exception e){
                    e.printStackTrace();
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
        }
    }
}
