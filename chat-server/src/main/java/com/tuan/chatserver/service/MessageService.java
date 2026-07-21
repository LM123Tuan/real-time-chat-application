package com.tuan.chatserver.service;

import com.tuan.chatserver.document.Message;
import com.tuan.chatserver.dto.MessageDTO;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.enums.MessageStatus;
import com.tuan.chatserver.mapper.MessageMapper;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ liên quan đến {@link Message} (tin nhắn trong các đoạn chat).
 * <p>
 * Bao gồm các chức năng: truy vấn tin nhắn theo điều kiện khác nhau (chatbox, người gửi, khoảng thời gian),
 * gửi tin nhắn mới, cập nhật trạng thái tin nhắn (SENT → RECEIVED → SEEN), và thu hồi tin nhắn.
 * Service tích hợp với MongoDB (lưu trữ tin nhắn) và PostgreSQL (lưu trữ chatbox, user).
 */
@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatBoxRepository chatBoxRepository;
    private final UserRepository userRepository;
    private final MongoTransactionManager mongoTransactionManager;
    private final MessageMapper messageMapper;

    /**
     * Khởi tạo {@code MessageService} thông qua Constructor Injection.
     *
     * @param messageRepository        repository dùng để thao tác dữ liệu {@link Message} (MongoDB)
     * @param chatBoxRepository        repository dùng để thao tác dữ liệu {@link ChatBox} (PostgreSQL)
     * @param userRepository           repository dùng để thao tác dữ liệu {@link User} (PostgreSQL)
     * @param mongoTransactionManager  quản lý transaction cho MongoDB
     * @param messageMapper            mapper chuyển đổi {@link Message} sang {@link MessageDTO}
     */
    @Autowired
    public MessageService(MessageRepository messageRepository, ChatBoxRepository chatBoxRepository, UserRepository userRepository, MongoTransactionManager mongoTransactionManager, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.chatBoxRepository = chatBoxRepository;
        this.userRepository = userRepository;
        this.mongoTransactionManager = mongoTransactionManager;
        this.messageMapper = messageMapper;
    }

    /**
     * Lấy danh sách tất cả tin nhắn trong một đoạn chat, sắp xếp giảm dần theo thời gian.
     *
     * @param chatBoxId id của đoạn chat (chatbox) cần truy vấn
     * @return danh sách {@link MessageDTO} chứa các tin nhắn của chatbox, sắp xếp theo timestamp giảm dần;
     *         trả về danh sách rỗng nếu không có tin nhắn nào
     */
    public List<MessageDTO> findByChatBoxIdOrderByTimestampDesc(Long chatBoxId) {
        List<Message> messages = messageRepository.findByChatBoxIdOrderByTimestampDesc(chatBoxId);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    /**
     * Lấy danh sách tin nhắn được gửi bởi một người dùng cụ thể trong một đoạn chat,
     * sắp xếp giảm dần theo thời gian.
     *
     * @param senderId  id của người gửi tin nhắn
     * @param chatBoxId id của đoạn chat cần truy vấn
     * @return danh sách {@link MessageDTO} chứa các tin nhắn do senderId gửi trong chatbox,
     *         sắp xếp theo timestamp giảm dần; trả về danh sách rỗng nếu không có tin nhắn nào
     */
    public List<MessageDTO> findBySenderIdAndChatBoxIdOrderByTimestampDesc(Long senderId, Long chatBoxId) {
        List<Message> messages = messageRepository.findBySenderIdAndChatBoxIdOrderByTimestampDesc(senderId, chatBoxId);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    /**
     * Lấy danh sách tin nhắn trong một đoạn chat nằm trong khoảng thời gian nhất định,
     * sắp xếp giảm dần theo thời gian.
     *
     * @param chatBoxId id của đoạn chat cần truy vấn
     * @param startTime thời gian bắt đầu của khoảng thời gian (inclusive)
     * @param endTime   thời gian kết thúc của khoảng thời gian (inclusive)
     * @return danh sách {@link MessageDTO} chứa các tin nhắn trong khoảng thời gian đã chỉ định,
     *         sắp xếp theo timestamp giảm dần; trả về danh sách rỗng nếu không có tin nhắn nào
     */
    public List<MessageDTO> findByChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime){
        List<Message> messages = messageRepository.findByChatBoxIdAndTimestampBetweenOrderByTimestampDesc(chatBoxId,startTime,endTime);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    /**
     * Lấy danh sách tin nhắn do một người gửi cụ thể gửi trong một đoạn chat,
     * nằm trong khoảng thời gian nhất định, sắp xếp giảm dần theo thời gian.
     *
     * @param senderId  id của người gửi tin nhắn
     * @param chatBoxId id của đoạn chat cần truy vấn
     * @param startTime thời gian bắt đầu của khoảng thời gian (inclusive)
     * @param endTime   thời gian kết thúc của khoảng thời gian (inclusive)
     * @return danh sách {@link MessageDTO} chứa các tin nhắn do senderId gửi trong khoảng thời gian,
     *         sắp xếp theo timestamp giảm dần; trả về danh sách rỗng nếu không có tin nhắn nào
     */
    public List<MessageDTO> findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampDesc(Long senderId, Long chatBoxId, LocalDateTime startTime, LocalDateTime endTime){
        List<Message> messages = messageRepository.findBySenderIdAndChatBoxIdAndTimestampBetweenOrderByTimestampDesc(senderId,chatBoxId,startTime,endTime);
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = messageMapper.mapMessageToMessageDTO(message);
            messageDTOs.add(messageDTO);
        }
        return messageDTOs;
    }

    /**
     * Gửi một tin nhắn mới vào một đoạn chat.
     * <p>
     * Phương thức kiểm tra:
     * <ul>
     *     <li>Đoạn chat tồn tại trong hệ thống.</li>
     *     <li>Người gửi tồn tại và là thành viên của đoạn chat.</li>
     *     <li>Nội dung tin nhắn không rỗng.</li>
     * </ul>
     * Nếu thỏa mãn tất cả điều kiện, tin nhắn sẽ được tạo với trạng thái {@link MessageStatus#SENT},
     * đồng thời thời gian hoạt động gần nhất ({@code lastActiveTime}) của chatbox sẽ được cập nhật.
     * <p>
     * Phương thức được đánh dấu với {@code @Transactional} để đảm bảo tính nhất quán dữ liệu.
     *
     * @param senderId  id của người gửi tin nhắn
     * @param chatBoxId id của đoạn chat mà tin nhắn sẽ được gửi đến
     * @param content   nội dung của tin nhắn (không được để trống)
     * @return {@code true} nếu gửi và lưu tin nhắn thành công; {@code false} nếu chatbox/sender
     *         không tồn tại, sender không phải thành viên của chatbox, nội dung rỗng, hoặc xảy ra lỗi
     * @throws NoSuchElementException nếu không tìm thấy chatbox hoặc sender trong hệ thống
     */
    @Transactional
    public boolean sendMessage(Long senderId, Long chatBoxId, String content){
        ChatBox chatBox= chatBoxRepository.findById(chatBoxId).orElseThrow(() -> new NoSuchElementException("Cannot find chatbox"));
        User sender=userRepository.findById(senderId).orElseThrow(() -> new NoSuchElementException("Cannot find sender"));
        if(chatBox.getUsers().contains(sender)){
            if(!content.isEmpty()){
                chatBox.setLastActiveTime(LocalDateTime.now());
                chatBoxRepository.save(chatBox);
                Message message=new Message(senderId, chatBoxId, LocalDateTime.now(), true, MessageStatus.SENT, content);
                messageRepository.save(message);
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    /**
     * Cập nhật trạng thái của một tin nhắn theo quy trình: SENT → RECEIVED → SEEN.
     * <p>
     * Mỗi lần gọi phương thức sẽ nâng cấp trạng thái tin nhắn lên một bậc.
     * Nếu tin nhắn đã ở trạng thái {@link MessageStatus#SEEN}, trạng thái sẽ không thay đổi
     * và phương thức trả về {@code false}.
     *
     * @param messageId id (ObjectId) của tin nhắn cần cập nhật trạng thái
     * @return {@code true} nếu cập nhật trạng thái và lưu thành công; {@code false} nếu
     *         tin nhắn không tồn tại, đã ở trạng thái SEEN, hoặc xảy ra lỗi khi lưu
     */
    public boolean updateMessageStatus(String messageId){
        Optional<Message> message=messageRepository.findById(messageId);
        if(message.isPresent()){
            Message actualMessage=message.get();
            MessageStatus messageStatus=actualMessage.getStatus();
            if(messageStatus == MessageStatus.SENT){
                messageStatus=MessageStatus.RECEIVED;
            }else if(messageStatus == MessageStatus.RECEIVED){
                messageStatus=MessageStatus.SEEN;
            }else if(messageStatus == MessageStatus.SEEN){
                return false;
            }
            actualMessage.setStatus(messageStatus);
            try{
                messageRepository.save(actualMessage);
                return true;
            }catch(Exception e){
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
        }
    }

    /**
     * Thu hồi (recall/delete) một tin nhắn bằng cách đánh dấu nó là không xem được ({@code viewable = false}).
     * <p>
     * Tin nhắn chỉ có thể được thu hồi nếu đang ở trạng thái xem được ({@code viewable = true}).
     * Sau khi thu hồi, tin nhắn sẽ không còn hiển thị cho người nhận.
     *
     * @param messageId id (ObjectId) của tin nhắn cần thu hồi
     * @return {@code true} nếu thu hồi và lưu thành công; {@code false} nếu tin nhắn không tồn tại,
     *         đã bị thu hồi (viewable = false), hoặc xảy ra lỗi khi lưu
     */
    public boolean recallMessage(String messageId){
        Optional<Message> message=messageRepository.findById(messageId);
        if(message.isPresent()){
            Message actualMessage=message.get();
            boolean messageViewable=actualMessage.isViewable();
            if(messageViewable){
                actualMessage.setViewable(false);
                try{
                    messageRepository.save(actualMessage);
                    return true;
                }catch(Exception e){
                    e.printStackTrace();
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
        }
    }
}