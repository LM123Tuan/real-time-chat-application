package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.exception.UserNotFoundException;
import com.tuan.chatserver.exception.UserNotInChatBoxException;
import com.tuan.chatserver.mapper.DirectMessageMapper;
import com.tuan.chatserver.exception.ChatBoxAlreadyExistsException;
import com.tuan.chatserver.repository.DirectMessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DirectMessageService {
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DirectMessageService(DirectMessageRepository directMessageRepository, UserRepository userRepository) {
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
    }

    public DirectMessageDTO createDirectMessage(Long creatorId, Long receiverId) {
        logger.info("Create direct message attempt between userId: {} and userId: {}", creatorId, receiverId);
        User creator = userRepository.findById(creatorId).orElseThrow(() -> {
            logger.warn("Create direct message failed - creator not found: creatorId={}", creatorId);
            throw new UserNotFoundException(creatorId);
        });
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> {
            logger.warn("Create direct message failed - receiver not found: receiverId={}", receiverId);
            throw new UserNotFoundException(receiverId);
        });
        if(directMessageRepository.existsBetweenTwoUsers(creatorId,receiverId)){
            logger.warn("Create direct message failed - already exists between userId: {} and userId: {}", creatorId, receiverId);
            throw new ChatBoxAlreadyExistsException();
        }
        Set<User> users=new HashSet<>();
        users.add(creator);
        users.add(receiver);
        String name = creator.getUsername() + " & " + receiver.getUsername();
        DirectMessage directMessage=new DirectMessage(name, LocalDateTime.now(), users, true, LocalDateTime.now());
        try{
            directMessageRepository.save(directMessage);
            logger.info("Create direct message successful between userId: {} and userId: {}", creatorId, receiverId);
            DirectMessageDTO dto = DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage);
            return dto;
        }catch(Exception e){
            logger.error("Create direct message failed while saving between userId: {} and userId: {}", creatorId, receiverId, e);
            throw new DataAccessFailureException(e);
        }
    }

    public DirectMessageDTO getChatBetweenTwoUsersByChatBoxId(Long userId, Long id){
        logger.debug("Fetching direct message with chatBoxId: {}", id);
        Optional<DirectMessage> directMessage= directMessageRepository.findById(id);
        if(directMessage.isPresent()){
            User user = userRepository.findById(id).orElseThrow(() -> {
                throw new UserNotFoundException(id);
            });
            if(!directMessage.get().getUsers().contains(user)){
                throw new UserNotInChatBoxException(id, userId);
            }
            return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage.get());
        }else{
            logger.warn("Get direct message failed - chatBoxId not found: {}", id);
            return null;
        }
    }

    public List<DirectMessageDTO> getAllChatByUserId(Long userId){
        logger.debug("Fetching active direct messages for userId: {}", userId);
        List<DirectMessage> directMessages = directMessageRepository.findByUserIdAndIsActiveTrue(userId);
        List<DirectMessageDTO> directMessageDTOS=new ArrayList<>();
        for(DirectMessage directMessage:directMessages){
            DirectMessageDTO directMessageDTO=DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage);
            directMessageDTOS.add(directMessageDTO);
        }
        logger.debug("Found {} active direct message(s) for userId: {}", directMessageDTOS.size(), userId);
        return directMessageDTOS;
    }
}