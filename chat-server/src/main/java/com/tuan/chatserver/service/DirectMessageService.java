package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.DirectMessageDTO;
import com.tuan.chatserver.entity.DirectMessage;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.ChatBoxNotFoundException;
import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.exception.UserNotFoundException;
import com.tuan.chatserver.mapper.DirectMessageMapper;
import com.tuan.chatserver.exception.ChatBoxAlreadyExistsException;
import com.tuan.chatserver.repository.DirectMessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
     * @throws UserNotFoundException         nếu creator hoặc receiver không tồn tại
     * @throws ChatBoxAlreadyExistsException nếu đã tồn tại đoạn chat giữa hai người dùng này
     * @throws DataAccessFailureException    nếu lỗi khi lưu vào database
     */
    public void createDirectMessage(Long creatorId, Long receiverId) {
        logger.info("Create direct message attempt between userId: {} and userId: {}", creatorId, receiverId);
        User creator = userRepository.findById(creatorId).orElseThrow(() -> {
            logger.warn("Create direct message failed - creator not found: creatorId={}", creatorId);
            throw new UserNotFoundException(creatorId);
        });
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> {
            logger.warn("Create direct message failed - receiver not found: receiverId={}", receiverId);
            throw new UserNotFoundException(receiverId);
        });
        if(directMessageRepository.existsBetweenTwoUsers(creatorId,receiverId)){
            logger.warn("Create direct message failed - already exists between userId: {} and userId: {}", creatorId, receiverId);
            throw new ChatBoxAlreadyExistsException();
        }
        Set<User> users=new HashSet<>();
        users.add(creator);
        users.add(receiver);
        DirectMessage directMessage=new DirectMessage(LocalDateTime.now(), users, true, LocalDateTime.now());
        try{
            directMessageRepository.save(directMessage);
            logger.info("Create direct message successful between userId: {} and userId: {}", creatorId, receiverId);
        }catch(Exception e){
            logger.error("Create direct message failed while saving between userId: {} and userId: {}", creatorId, receiverId, e);
            throw new DataAccessFailureException(e);
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
        logger.debug("Fetching direct message between userId: {} and userId: {}", userId1, userId2);
        Optional<DirectMessage> directMessage = directMessageRepository.findBetweenTwoUsers(userId1,userId2);
        if(directMessage.isPresent()){
            return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage.get());
        }else{
            logger.warn("Get direct message failed - not found between userId: {} and userId: {}", userId1, userId2);
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
        logger.debug("Fetching direct message with chatBoxId: {}", id);
        Optional<DirectMessage> directMessage= directMessageRepository.findById(id);
        if(directMessage.isPresent()){
            return DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage.get());
        }else{
            logger.warn("Get direct message failed - chatBoxId not found: {}", id);
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
        logger.debug("Fetching active direct messages for userId: {}", userId);
        List<DirectMessage> directMessages = directMessageRepository.findByUserIdAndIsActiveTrue(userId);
        List<DirectMessageDTO> directMessageDTOS=new ArrayList<>();
        for(DirectMessage directMessage:directMessages){
            DirectMessageDTO directMessageDTO=DirectMessageMapper.mapDirectMessageToDirectMessageDTO(directMessage);
            directMessageDTOS.add(directMessageDTO);
        }
        logger.debug("Found {} active direct message(s) for userId: {}", directMessageDTOS.size(), userId);
        return directMessageDTOS;
    }
}