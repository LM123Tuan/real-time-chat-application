package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatBoxService {
    private final ChatBoxRepository chatBoxRepository;

    @Autowired
    public ChatBoxService(ChatBoxRepository chatBoxRepository) {
        this.chatBoxRepository = chatBoxRepository;
    }

    public List<ChatBoxDTO> getAllChatboxForUser(Long userId) {
        List<ChatBox> chatBoxes= chatBoxRepository.findByUsers_IdOrderByLastActiveTimeDesc(userId);
        List<ChatBoxDTO> chatBoxDTOS=new ArrayList<>();
        for(ChatBox chatBox:chatBoxes){
            ChatBoxDTO chatBoxDTO= ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }
        return chatBoxDTOS;
    }
}
