package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý nghiệp vụ liên quan đến {@link ChatBox} (đoạn chat).
 * <p>
 * Cung cấp chức năng truy vấn danh sách chatbox của một người dùng,
 * sắp xếp theo thời gian hoạt động gần nhất.
 */
@Service
public class ChatBoxService {
    private final ChatBoxRepository chatBoxRepository;

    /**
     * Khởi tạo {@code ChatBoxService} thông qua Constructor Injection.
     *
     * @param chatBoxRepository repository dùng để thao tác dữ liệu {@link ChatBox}
     */
    @Autowired
    public ChatBoxService(ChatBoxRepository chatBoxRepository) {
        this.chatBoxRepository = chatBoxRepository;
    }

    /**
     * Lấy danh sách tất cả các chatbox đang hoạt động mà một người dùng tham gia,
     * sắp xếp giảm dần theo thời gian hoạt động gần nhất.
     *
     * @param userId id của người dùng cần truy vấn danh sách chatbox
     * @return danh sách {@link ChatBoxDTO} tương ứng với các chatbox đang hoạt động
     *         mà người dùng tham gia; trả về danh sách rỗng nếu không có chatbox nào
     */
    public List<ChatBoxDTO> getAllChatboxForUser(Long userId) {
        List<ChatBox> chatBoxes= chatBoxRepository.findByUserIdAndIsActiveTrueOrderByLastActiveTimeDesc(userId);
        List<ChatBoxDTO> chatBoxDTOS=new ArrayList<>();
        for(ChatBox chatBox:chatBoxes){
            ChatBoxDTO chatBoxDTO= ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }
        return chatBoxDTOS;
    }
}