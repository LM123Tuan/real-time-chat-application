package com.tuan.chatserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.dto.PageCursor;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.EntityType;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DirectMessageService {
    private static final String PRIVATE_CHAT_KEY_PREFIX = "privatechatkey:";
    private static final Duration CREATE_PRIVATE_CHAT_TTL = Duration.ofSeconds(5);
    private final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] " +
                    "then " +
                        "return redis.call('DEL', KEYS[1]) " +
                    "else " +
                        "return 0 " +
                    "end",
            Long.class
    );
    private final RedisTemplate<String, Object> redisTemplate;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final CursorCodec cursorCodec;
    private final RedisService redisService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public DirectMessageService(DirectMessageRepository directMessageRepository,
                                UserRepository userRepository,
                                MessageRepository messageRepository,
                                CursorCodec cursorCodec,
                                RedisService redisService,
                                RedisTemplate<String, Object> redisTemplate) {
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.cursorCodec=cursorCodec;
        this.redisService=redisService;
        this.redisTemplate=redisTemplate;
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
        String lockKey = PRIVATE_CHAT_KEY_PREFIX+smallerId+","+largerId;
        String lockValue = UUID.randomUUID().toString();

        boolean acquired = redisService.setIfAbsent(lockKey, lockValue, CREATE_PRIVATE_CHAT_TTL);
        if(!acquired){
            throw new LockTimeoutException(EntityType.DIRECT_MESSAGE, null);
        }
        try{
            if(directMessageRepository.existsBetweenTwoUsers(creatorId, receiverId)){
                throw new ChatBoxAlreadyExistsException();
            }

            Set<User> users=new HashSet<>();
            users.add(creator);
            users.add(receiver);
            String name = creator.getUsername() + " & " + receiver.getUsername();
            String conversationKey=smallerId + "_" + largerId;
            DirectMessage directMessage=new DirectMessage(name, LocalDateTime.now(), users, true, LocalDateTime.now(), conversationKey);
            directMessageRepository.save(directMessage);
            logger.info("Create direct message successful between userId: {} and userId: {}", creatorId, receiverId);
            return directMessage;
        }catch (ChatBoxAlreadyExistsException e) {
            throw e;
        }catch (DataIntegrityViolationException e) {
            logger.warn("Create direct message failed - constraint violation, likely race condition between userId: {} and userId: {}", creatorId, receiverId);
            throw new ChatBoxAlreadyExistsException();
        }catch (Exception e) {
            logger.error("Create direct message failed while saving between userId: {} and userId: {}", creatorId, receiverId, e);
            throw new DataAccessFailureException(e);
        }finally{
            Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
            if (result == null || result == 0) {
                logger.warn("Lock was not released, either already expired or held by another owner, lockKey={}", lockKey);
            } else {
                logger.info("Successfully released lock, lockKey={}", lockKey);
            }
        }
    }

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

    public CursorPaginationResponse<List<DirectMessageDTO>> getAllChatByUserId(Long userId, CursorPaginationRequest request) {
        logger.debug("Fetching active direct messages for userId: {}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<DirectMessage> directMessagesRaw;
        if (request.getCursor() == null) {
            directMessagesRaw = directMessageRepository.findByUserIdOfFirstPage(userId, pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(
                    request.getCursor(), new TypeReference<PageCursor<Long>>() {});
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
            PageCursor<Long> cursorData = new PageCursor<>(lastDm.getLastActiveTime(), lastDm.getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<DirectMessageDTO>> response =
                new CursorPaginationResponse<>(directMessageDTOs, nextCursor, hasNext);

        logger.debug("Found {} direct message(s) for userId: {}", directMessageDTOs.size(), userId);
        return response;
    }
}