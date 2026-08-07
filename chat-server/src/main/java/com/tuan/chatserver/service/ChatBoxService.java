package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.enums.ChatboxType;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatBoxService {

    private final ChatBoxRepository chatBoxRepository;
    private final MessageRepository messageRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ChatBoxService(ChatBoxRepository chatBoxRepository,
                          MessageRepository messageRepository) {
        this.chatBoxRepository = chatBoxRepository;
        this.messageRepository = messageRepository;
    }

    public CursorPaginationResponse<List<ChatBoxDTO>, Long> getAllChatboxesForUser(Long userId, CursorPaginationRequest<Long> request) {
        logger.debug("Fetching active chatboxes for userId: {}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<ChatBox> chatBoxes;
        if (request.getCursorTimestamp() == null) {
            chatBoxes = chatBoxRepository.findByUserIdAndIsActiveTrueOfFirstPage(userId, pageable);
        } else {
            chatBoxes = chatBoxRepository.findByUserIdAndIsActiveTrueOfNextPage(
                    userId,
                    request.getCursorTimestamp(),
                    request.getCursorId(),
                    pageable
            );
        }

        boolean hasNext = chatBoxes.size() > request.getSize();
        if (hasNext) {
            chatBoxes = chatBoxes.subList(0, request.getSize());
        }

        Set<Long> directMessageChatBoxIds = chatBoxes.stream()
                .filter(cb -> cb.getChatboxType() == ChatboxType.DIRECT_MESSAGE)
                .map(ChatBox::getId)
                .collect(Collectors.toSet());

        Set<Long> chatBoxIdsWithMessages = directMessageChatBoxIds.isEmpty()
                ? Set.of()
                : new HashSet<>(messageRepository.findDistinctChatBoxIdByChatBoxIdIn(new ArrayList<>(directMessageChatBoxIds)));

        Set<Long> chatBoxIds = new HashSet<>();
        for (ChatBox chatBox : chatBoxes) {
            if (chatBox.getChatboxType() == ChatboxType.DIRECT_MESSAGE
                    && !chatBoxIdsWithMessages.contains(chatBox.getId())) {
                continue;
            }
            chatBoxIds.add(chatBox.getId());
        }

        List<ChatBox> confirmedChatBoxes = chatBoxIds.isEmpty()
                ? List.of()
                : chatBoxRepository.findByIdInWithUsers(new ArrayList<>(chatBoxIds));

        Map<Long, ChatBox> confirmedChatBoxMap = confirmedChatBoxes.stream()
                .collect(Collectors.toMap(ChatBox::getId, cb -> cb));

        List<ChatBoxDTO> chatBoxDTOs = new ArrayList<>();
        for (ChatBox chatBox : chatBoxes) {
            ChatBox confirmed = confirmedChatBoxMap.get(chatBox.getId());
            if (confirmed != null) {
                chatBoxDTOs.add(ChatBoxMapper.mapChatBoxToChatBoxDTO(confirmed));
            }
        }

        LocalDateTime nextTimestamp = null;
        Long nextCursor = null;
        if (!chatBoxes.isEmpty()) {
            ChatBox lastChatBox = chatBoxes.get(chatBoxes.size() - 1);
            nextTimestamp = lastChatBox.getLastActiveTime();
            nextCursor = lastChatBox.getId();
        }

        CursorPaginationResponse<List<ChatBoxDTO>, Long> response =
                new CursorPaginationResponse<>(chatBoxDTOs, nextTimestamp, nextCursor, hasNext);

        logger.debug("Found {} active chatbox(es) for userId: {}", chatBoxDTOs.size(), userId);
        return response;
    }
}