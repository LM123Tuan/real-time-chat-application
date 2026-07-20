package com.tuan.chatserver.mapper;

import com.tuan.chatserver.document.Message;
import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.MessageDTO;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.MessageStatus;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class MessageMapper {
    private final UserRepository userRepository;
    private final ChatBoxRepository chatBoxRepository;

    public MessageMapper(UserRepository userRepository, ChatBoxRepository chatBoxRepository) {
        this.userRepository = userRepository;
        this.chatBoxRepository=chatBoxRepository;
    }
    public MessageDTO mapMessageToMessageDTO(Message message) {
        UserDTO senderDTO=new UserDTO();
        ChatBoxDTO chatBoxDTO=new ChatBoxDTO();
        String id=message.getId();
        Long senderId=message.getSenderId();
        Optional<User> sender=userRepository.findById(senderId);
        if(sender.isPresent()){
            User actualSender=sender.get();
            senderDTO = UserMapper.mapUserToUserDTO(actualSender);
        }else{
            return null;
        }
        Long chatBoxId=message.getChatBoxId();
        Optional<ChatBox> chatBox=chatBoxRepository.findById(chatBoxId);
        if(chatBox.isPresent()){
            ChatBox actualChatBox=chatBox.get();
            chatBoxDTO=ChatBoxMapper.mapChatBoxToChatBoxDTO(actualChatBox);
        }else{
            return null;
        }
        LocalDateTime timestamp=message.getTimestamp();
        MessageStatus status=message.getStatus();
        String content=message.getContent();
        return new MessageDTO(id, senderDTO, chatBoxDTO, timestamp, status, content);
    }
}
