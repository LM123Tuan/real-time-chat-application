package com.tuan.chatserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.CursorPaginationRequest;
import com.tuan.chatserver.dto.CursorPaginationResponse;
import com.tuan.chatserver.dto.PageCursor;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.enums.ChatboxType;
import com.tuan.chatserver.exception.ChatBoxNotFoundException;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.util.CursorCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatBoxService {

    private final ChatBoxRepository chatBoxRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final CursorCodec cursorCodec;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ChatBoxService(ChatBoxRepository chatBoxRepository,
                          MessageRepository messageRepository,
                          MessageService messageService,
                          CursorCodec cursorCodec) {
        this.chatBoxRepository = chatBoxRepository;
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.cursorCodec=cursorCodec;
    }

    public CursorPaginationResponse<List<ChatBoxDTO>> getAllChatboxesForUser(Long userId, CursorPaginationRequest request) {
        logger.debug("Fetching active chatboxes for userId: {}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<ChatBox> chatBoxes;
        if (request.getCursor() == null) {
            chatBoxes = chatBoxRepository.findByUserIdAndIsActiveTrueOfFirstPage(userId, pageable);
        } else {
            PageCursor<Long> cursorData = cursorCodec.decode(request.getCursor(), new TypeReference<PageCursor<Long>>(){});
            chatBoxes = chatBoxRepository.findByUserIdAndIsActiveTrueOfNextPage(
                    userId,
                    cursorData.getTimestamp(),
                    cursorData.getId(),
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

        messageService.markSentMessagesAsReceivedForChatBoxes(userId, confirmedChatBoxes);

        Map<Long, ChatBox> confirmedChatBoxMap = confirmedChatBoxes.stream()
                .collect(Collectors.toMap(ChatBox::getId, cb -> cb));

        List<ChatBoxDTO> chatBoxDTOs = new ArrayList<>();
        for (ChatBox chatBox : chatBoxes) {
            ChatBox confirmed = confirmedChatBoxMap.get(chatBox.getId());
            if (confirmed != null) {
                chatBoxDTOs.add(ChatBoxMapper.mapChatBoxToChatBoxDTO(confirmed));
            }
        }

        String nextCursor = null;
        if (!chatBoxes.isEmpty()) {
            ChatBox lastChatBox = chatBoxes.get(chatBoxes.size() - 1);
            PageCursor<Long> cursorData = new PageCursor<>(lastChatBox.getLastActiveTime(), lastChatBox.getId());
            nextCursor = cursorCodec.encode(cursorData);
        }

        CursorPaginationResponse<List<ChatBoxDTO>> response =
                new CursorPaginationResponse<>(chatBoxDTOs, nextCursor, hasNext);

        logger.debug("Found {} active chatbox(es) for userId: {}", chatBoxDTOs.size(), userId);
        return response;
    }

    public ChatBox getChatBoxWithUsers(Long chatBoxId) {
        logger.debug("Fetching chatbox with users, chatBoxId={}", chatBoxId);
        return chatBoxRepository.findById(chatBoxId)
                .orElseThrow(() -> {
                    logger.warn("Validation failed: chatbox not found, chatBoxId={}", chatBoxId);
                    return new ChatBoxNotFoundException(chatBoxId);
                });
    }
}