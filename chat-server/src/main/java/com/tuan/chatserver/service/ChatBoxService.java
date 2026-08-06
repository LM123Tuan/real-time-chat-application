package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.enums.ChatboxType;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public List<ChatBoxDTO> getAllChatboxesForUser(Long userId) {
        logger.debug("Fetching active chatboxes for userId: {}", userId);
        List<ChatBox> chatBoxes = chatBoxRepository.findByUserIdAndIsActiveTrueOrderByLastActiveTimeDesc(userId);
        List<ChatBoxDTO> chatBoxDTOS = new ArrayList<>();

        for(ChatBox chatBox : chatBoxes){
            if(chatBox.getChatboxType().equals(ChatboxType.DIRECT_MESSAGE) && !messageRepository.existByChatBoxId(chatBox.getId())){
                continue;
            }
            ChatBoxDTO chatBoxDTO = ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }

        logger.debug("Found {} active chatbox(es) for userId: {}", chatBoxDTOS.size(), userId);
        return chatBoxDTOS;
    }
}