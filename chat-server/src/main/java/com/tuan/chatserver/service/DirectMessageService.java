package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.exception.UserNotFoundException;
import com.tuan.chatserver.exception.UserNotInChatBoxException;
import com.tuan.chatserver.mapper.DirectMessageMapper;
import com.tuan.chatserver.exception.ChatBoxAlreadyExistsException;
import com.tuan.chatserver.repository.DirectMessageRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DirectMessageService {
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DirectMessageService(DirectMessageRepository directMessageRepository,
                                UserRepository userRepository,
                                MessageRepository messageRepository) {
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
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

    public CursorPaginationResponse<List<DirectMessageDTO>, Long> getAllChatByUserId(Long userId, CursorPaginationRequest<Long> request) {
        logger.debug("Fetching active direct messages for userId: {}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<DirectMessage> directMessagesRaw;
        if (request.getCursorId() == null) {
            directMessagesRaw = directMessageRepository.findByUserIdOfFirstPage(userId, pageable);
        } else {
            directMessagesRaw = directMessageRepository.findByUserIdOfNextPage(
                    userId,
                    request.getCursorTimestamp(),
                    request.getCursorId(),
                    pageable
            );
        }

        boolean hasNext = directMessagesRaw.size() > request.getSize();

        List<DirectMessage> directMessages = hasNext
                ? directMessagesRaw.subList(0, request.getSize())
                : directMessagesRaw;

        Set<Long> directMessageIdsToCheck = directMessages.stream()
                .map(DirectMessage::getId)
                .collect(Collectors.toSet());

        Set<Long> chatBoxIdsWithMessages = directMessageIdsToCheck.isEmpty()
                ? Set.of()
                : new HashSet<>(messageRepository.findDistinctChatBoxIdByChatBoxIdIn(new ArrayList<>(directMessageIdsToCheck)));

        Set<Long> validDirectMessageIds = new HashSet<>();
        for (DirectMessage dm : directMessages) {
            if (!chatBoxIdsWithMessages.contains(dm.getId())) {
                continue;
            }
            validDirectMessageIds.add(dm.getId());
        }

        List<DirectMessage> confirmedDirectMessages = validDirectMessageIds.isEmpty()
                ? List.of()
                : directMessageRepository.findByIdInWithUsers(new ArrayList<>(validDirectMessageIds));

        Map<Long, DirectMessage> confirmedDirectMessageMap = confirmedDirectMessages.stream()
                .collect(Collectors.toMap(DirectMessage::getId, dm -> dm));

        List<DirectMessageDTO> directMessageDTOs = new ArrayList<>();
        for (DirectMessage dm : directMessages) {
            DirectMessage confirmed = confirmedDirectMessageMap.get(dm.getId());
            if (confirmed != null) {
                directMessageDTOs.add(DirectMessageMapper.mapDirectMessageToDirectMessageDTO(confirmed));
            }
        }

        LocalDateTime nextTimestamp = null;
        Long nextCursor = null;
        if (!directMessages.isEmpty()) {
            DirectMessage lastDm = directMessages.get(directMessages.size() - 1);
            nextTimestamp = lastDm.getLastActiveTime();
            nextCursor = lastDm.getId();
        }

        CursorPaginationResponse<List<DirectMessageDTO>, Long> response =
                new CursorPaginationResponse<>(directMessageDTOs, nextTimestamp, nextCursor, hasNext);

        logger.debug("Found {} direct message(s) for userId: {}", directMessageDTOs.size(), userId);
        return response;
    }
}