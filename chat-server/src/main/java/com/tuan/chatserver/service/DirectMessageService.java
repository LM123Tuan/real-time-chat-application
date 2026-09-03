package com.tuan.chatserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.dto.PageCursor;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.DirectMessageMapper;
import com.tuan.chatserver.repository.DirectMessageRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import com.tuan.chatserver.util.CursorCodec;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final CursorCodec cursorCodec;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DirectMessageService(DirectMessageRepository directMessageRepository,
                                UserRepository userRepository,
                                MessageRepository messageRepository,
                                CursorCodec cursorCodec) {
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.cursorCodec=cursorCodec;
    }

    private DirectMessage createDirectMessageIfNotExists(Long creatorId, Long receiverId) {
        logger.info("Create direct message attempt between userId: {} and userId: {}", creatorId, receiverId);
        User creator = userRepository.findById(creatorId).orElseThrow(() -> {
            logger.warn("Create direct message failed - creator not found: creatorId={}", creatorId);
            throw new UserNotFoundException(creatorId);
        });
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> {
            logger.warn("Create direct message failed - receiver not found: receiverId={}", receiverId);
            throw new UserNotFoundException(receiverId);
        });

        Long smallerId = Math.min(creatorId, receiverId);
        Long largerId = Math.max(creatorId, receiverId);

        try {
            if (directMessageRepository.existsBetweenTwoUsers(creatorId, receiverId)) {
                throw new ChatBoxAlreadyExistsException();
            }

            Set<User> users = new HashSet<>();
            users.add(creator);
            users.add(receiver);
            String name = creator.getUsername() + " & " + receiver.getUsername();
            String conversationKey = smallerId + "_" + largerId;
            DirectMessage directMessage = new DirectMessage(name, LocalDateTime.now(), users, true, LocalDateTime.now(), conversationKey);
            directMessageRepository.save(directMessage);
            logger.info("Create direct message successful between userId: {} and userId: {}", creatorId, receiverId);
            return directMessage;
        } catch (ChatBoxAlreadyExistsException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            logger.warn("Create direct message failed - constraint violation, likely race condition between userId: {} and userId: {}", creatorId, receiverId);
            throw new ChatBoxAlreadyExistsException();
        } catch (Exception e) {
            logger.error("Create direct message failed while saving between userId: {} and userId: {}", creatorId, receiverId, e);
            throw new DataAccessFailureException(e);
        }
    }

    @Cacheable(
            value = "directMessage_UserIds",
            key = "T(Math).min(#requesterId, #receiverId) + '_' + T(Math).max(#requesterId, #receiverId)"
    )
    @Transactional
    public DirectMessageDTO getChatBetweenTwoUsersByUserIds(Long requesterId, Long receiverId) {
        logger.info("Attempting to get direct message between users, requesterId={}, receiverId={}", requesterId, receiverId);

        userRepository.findById(requesterId).orElseThrow(() -> new UserNotFoundException(requesterId));
        userRepository.findById(receiverId).orElseThrow(() -> new UserNotFoundException(receiverId));

        DirectMessage directMessage = directMessageRepository.findBetweenTwoUsers(requesterId, receiverId)
                .orElseGet(() -> createDirectMessageIfNotExists(requesterId, receiverId));

        logger.info("Successfully retrieved direct message, directMessageId={}", directMessage.getId());
        return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage);
    }

    @Cacheable(value="directMessage_chatBoxId", key = "#userId + '_' + #id")
    public DirectMessageDTO getChatBetweenTwoUsersByChatBoxId(Long userId, Long id) {
        logger.debug("Fetching direct message with chatBoxId: {}", id);
        DirectMessage directMessage = directMessageRepository.findById(id)
                .orElseThrow(() -> new ChatBoxNotFoundException(id));

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!directMessage.getUsers().contains(user)) {
            throw new UserNotInChatBoxException(id, userId);
        }
        return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage);
    }

    @Cacheable(
            value = "user_DirectMessages",
            key = "#userId + '_' + @cursorHelper.extractPageNumber(#request.cursor)",
            condition = "@cursorHelper.extractPageNumber(#request.cursor) < 5"
    )
    public CursorPaginationResponse<List<DirectMessageDTO>> getAllChatByUserId(Long userId, CursorPaginationRequest request) {
        logger.debug("Fetching active direct messages for userId: {}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<DirectMessage> directMessagesRaw;
        long pageNumber=0;
        if (request.getCursor() == null) {
            directMessagesRaw = directMessageRepository.findByUserIdOfFirstPage(userId, pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(
                    request.getCursor(), new TypeReference<PageCursor<Long>>() {});
            pageNumber=cursorData.getPageNumber();
            directMessagesRaw = directMessageRepository.findByUserIdOfNextPage(
                    userId,
                    cursorData.getTimestamp(),
                    cursorData.getId(),
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

        String nextCursor = null;
        if (!directMessages.isEmpty()) {
            DirectMessage lastDm = directMessages.get(directMessages.size() - 1);
            PageCursor<Long> cursorData = new PageCursor<>(pageNumber+1, lastDm.getLastActiveTime(), lastDm.getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<DirectMessageDTO>> response =
                new CursorPaginationResponse<>(directMessageDTOs, nextCursor, hasNext);

        logger.debug("Found {} direct message(s) for userId: {}", directMessageDTOs.size(), userId);
        return response;
    }
}