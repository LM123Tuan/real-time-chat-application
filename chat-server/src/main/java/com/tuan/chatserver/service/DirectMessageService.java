package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.mapper.DirectMessageMapper;
import com.tuan.chatserver.repository.DirectMessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý nghiệp vụ liên quan đến {@link DirectMessage} (đoạn chat riêng tư giữa 2 người dùng).
 * <p>
 * Bao gồm các chức năng: tạo mới đoạn chat riêng, truy vấn đoạn chat theo cặp người dùng,
 * truy vấn theo id của chatbox, và lấy danh sách tất cả đoạn chat đang hoạt động của một người dùng.
 */
@Service
public class DirectMessageService {
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    /**
     * Khởi tạo {@code DirectMessageService} thông qua Constructor Injection.
     *
     * @param directMessageRepository repository dùng để thao tác dữ liệu {@link DirectMessage}
     * @param userRepository          repository dùng để thao tác dữ liệu {@link User}
     */
    @Autowired
    public DirectMessageService(DirectMessageRepository directMessageRepository, UserRepository userRepository) {
        this.directMessageRepository = directMessageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Tạo một đoạn chat riêng (direct message) mới giữa hai người dùng.
     * <p>
     * Phương thức sẽ kiểm tra:
     * <ul>
     *     <li>Cả hai người dùng (creator và receiver) phải tồn tại trong hệ thống.</li>
     *     <li>Giữa hai người dùng này chưa tồn tại đoạn chat riêng nào trước đó.</li>
     * </ul>
     * Nếu thỏa mãn các điều kiện trên, một {@link DirectMessage} mới sẽ được tạo và lưu vào database.
     *
     * @param creatorId  id của người dùng tạo đoạn chat
     * @param receiverId id của người dùng nhận đoạn chat
     * @return {@code true} nếu tạo và lưu thành công; {@code false} nếu một trong hai người dùng
     *         không tồn tại, đã tồn tại đoạn chat giữa hai người, hoặc xảy ra lỗi khi lưu
     */
    public boolean createDirectMessage(Long creatorId, Long receiverId) {
        Optional<User> creator = userRepository.findById(creatorId);
        Optional<User> receiver = userRepository.findById(receiverId);
        if(creator.isPresent() && receiver.isPresent()){
            if(!directMessageRepository.existsBetweenTwoUsers(creatorId,receiverId)){
                User actualCreator = creator.get();
                User actualReceiver = receiver.get();
                Set<User> users=new HashSet<>();
                users.add(actualCreator);
                users.add(actualReceiver);
                DirectMessage directMessage=new DirectMessage(LocalDateTime.now(), users, true, LocalDateTime.now());
                try{
                    directMessageRepository.save(directMessage);
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

    /**
     * Lấy thông tin đoạn chat riêng giữa hai người dùng dựa trên id của họ.
     *
     * @param userId1 id của người dùng thứ nhất
     * @param userId2 id của người dùng thứ hai
     * @return {@link DirectMessageDTO} chứa thông tin đoạn chat nếu tồn tại;
     *         trả về {@code null} nếu không tìm thấy đoạn chat giữa hai người dùng này
     */
    public DirectMessageDTO getChatBetweenTwoUsersByUsersId(Long userId1,Long userId2){
        Optional<DirectMessage> directMessage = directMessageRepository.findBetweenTwoUsers(userId1,userId2);
        if(directMessage.isPresent()){
            return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage.get());
        }else{
            return null;
        }
    }

    /**
     * Lấy thông tin đoạn chat riêng dựa trên id của chatbox.
     *
     * @param id id của {@link DirectMessage} (chatbox) cần truy vấn
     * @return {@link DirectMessageDTO} chứa thông tin đoạn chat nếu tìm thấy;
     *         trả về {@code null} nếu không tồn tại chatbox với id tương ứng
     */
    public DirectMessageDTO getChatBetweenTwoUsersByChatBoxId(Long id){
        Optional<DirectMessage> directMessage= directMessageRepository.findById(id);
        if(directMessage.isPresent()){
            return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage.get());
        }else{
            return null;
        }
    }

    /**
     * Lấy danh sách tất cả các đoạn chat riêng đang hoạt động (active) của một người dùng.
     *
     * @param userId id của người dùng cần truy vấn danh sách đoạn chat
     * @return danh sách {@link DirectMessageDTO} tương ứng với các đoạn chat đang hoạt động
     *         của người dùng; trả về danh sách rỗng nếu không có đoạn chat nào
     */
    public List<DirectMessageDTO> getAllChatByUserId(Long userId){
        List<DirectMessage> directMessages = directMessageRepository.findByUserIdAndIsActiveTrue(userId);
        List<DirectMessageDTO> directMessageDTOS=new ArrayList<>();
        for(DirectMessage directMessage:directMessages){
            DirectMessageDTO directMessageDTO=DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage);
            directMessageDTOS.add(directMessageDTO);
        }
        return directMessageDTOS;
    }
}