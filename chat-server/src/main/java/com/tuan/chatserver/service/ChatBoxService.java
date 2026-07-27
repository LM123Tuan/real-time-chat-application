package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatBoxService {
    private final ChatBoxRepository chatBoxRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ChatBoxService(ChatBoxRepository chatBoxRepository) {
        this.chatBoxRepository = chatBoxRepository;
    }

    public List<ChatBoxDTO> getAllChatboxForUser(Long userId) {
        logger.debug("Fetching active chatboxes for userId: {}", userId);
        List<ChatBox> chatBoxes = chatBoxRepository.findByUserIdAndIsActiveTrueOrderByLastActiveTimeDesc(userId);
        List<ChatBoxDTO> chatBoxDTOS = new ArrayList<>();

        for(ChatBox chatBox : chatBoxes){
            ChatBoxDTO chatBoxDTO = ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }

        logger.debug("Found {} active chatbox(es) for userId: {}", chatBoxDTOS.size(), userId);
        return chatBoxDTOS;
    }
}