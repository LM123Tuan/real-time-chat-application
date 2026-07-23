//TODO
package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.AdminDTO;
import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.DataAccessFailureException;
import com.tuan.chatserver.exception.UserNotFoundException;
import com.tuan.chatserver.exception.UsernameOrEmailAlreadyExistsException;
import com.tuan.chatserver.mapper.AdminMapper;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.mapper.UserMapper;
import com.tuan.chatserver.repository.AdminRepository;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ liên quan đến quản lý hệ thống từ góc độ {@link Admin}.
 * <p>
 * Bao gồm các chức năng: quản lý tài khoản admin, quản lý người dùng (User),
 * quản lý chatbox, và thống kê các chỉ số của hệ thống (tổng số user, chatbox, message).
 */
@Service
public class AdminService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ChatBoxRepository chatBoxRepository;
    private final MessageRepository messageRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    /**
     * Khởi tạo {@code AdminService} thông qua Constructor Injection.
     *
     * @param userRepository        repository dùng để thao tác dữ liệu {@link User}
     * @param adminRepository       repository dùng để thao tác dữ liệu {@link Admin}
     * @param chatBoxRepository     repository dùng để thao tác dữ liệu {@link ChatBox}
     * @param messageRepository     repository dùng để thao tác dữ liệu {@link com.tuan.chatserver.document.Message}
     * @param bcryptPasswordEncoder encoder dùng để mã hóa mật khẩu
     */
    @Autowired
    public AdminService(UserRepository userRepository, AdminRepository adminRepository, ChatBoxRepository chatBoxRepository, MessageRepository messageRepository, BCryptPasswordEncoder bcryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.chatBoxRepository = chatBoxRepository;
        this.messageRepository = messageRepository;
        this.bcryptPasswordEncoder = bcryptPasswordEncoder;
    }

    //ADMIN

    /**
     * Đăng ký tài khoản admin mới.
     * <p>
     * Username phải duy nhất trong hệ thống. Mật khẩu được mã hóa bằng BCrypt
     * trước khi lưu. Tài khoản admin mới mặc định ở trạng thái hoạt động
     * ({@code isActive = true}).
     *
     * @param username username của tài khoản admin, phải duy nhất
     * @param password mật khẩu dạng plain text, sẽ được mã hóa trước khi lưu
     * @throws UsernameOrEmailAlreadyExistsException nếu username đã tồn tại
     * @throws DataAccessFailureException nếu xảy ra lỗi khi lưu dữ liệu
     */
    public void registerAdmin(String username, String password) {
        logger.info("Attempting to register admin, username={}", username);
        String hashedPassword = bcryptPasswordEncoder.encode(password);
        if(!adminRepository.existsByUsername(username)) {
            Admin admin = new Admin(username, hashedPassword, true);
            try{
                adminRepository.save(admin);
                logger.info("Admin registered successfully, username={}", username);
            }catch (Exception e){
                logger.error("Error occurred while registering admin, username={}", username, e);
                throw new DataAccessFailureException(e);
            }
        }else{
            logger.warn("Register admin failed: username already exists, username={}", username);
            throw new UsernameOrEmailAlreadyExistsException();
        }
    }

    /**
     * Tìm tài khoản admin theo username.
     *
     * @param username username của tài khoản admin cần tìm
     * @return {@link AdminDTO} chứa thông tin admin nếu tìm thấy;
     *         trả về {@code null} nếu không tồn tại admin với username tương ứng
     */
    public AdminDTO findAdminByUsername(String username) {
        logger.debug("Fetching admin by username, username={}", username);
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin for username={}", username);
            return adminDTO;
        } else {
            logger.debug("No admin found for username={}", username);
            return null;
        }
    }

    /**
     * Lấy thông tin hồ sơ (profile) của một tài khoản admin theo id.
     *
     * @param id id của tài khoản admin cần lấy thông tin
     * @return {@link AdminDTO} chứa thông tin admin; hoặc {@code null} nếu
     *         không tồn tại admin với id tương ứng
     */
    public AdminDTO getAdminProfile(Long id){
        logger.debug("Fetching admin profile, id={}", id);
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin profile for id={}", id);
            return adminDTO;
        } else {
            logger.debug("No admin found for id={}", id);
            return null;
        }
    }

    //USER

    /**
     * Lấy danh sách tất cả người dùng (User) trong hệ thống (kể cả những tài khoản đã bị vô hiệu hóa).
     *
     * @return danh sách {@link UserDTO} của tất cả người dùng;
     *         trả về danh sách rỗng nếu không có người dùng nào
     */
    public List<UserDTO> findAllUsers(){
        logger.debug("Fetching all users");
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOS = new ArrayList<>();
        for(User user:users){
            UserDTO userDTO = UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userDTO);
        }
        logger.debug("Found {} user(s)", userDTOS.size());
        return userDTOS;
    }

    /**
     * Tìm kiếm người dùng theo từ khóa trong username (LIKE search).
     *
     * @param keyword từ khóa tìm kiếm trong username
     * @return danh sách {@link UserDTO} của những người dùng khớp từ khóa
     *         (kể cả những tài khoản đã bị vô hiệu hóa); trả về danh sách rỗng
     *         nếu không có kết quả nào phù hợp
     */
    public List<UserDTO> findUserByUsernameContaining(String keyword){
        logger.debug("Fetching users by username containing keyword, keyword={}", keyword);
        List<User> users = userRepository.findByUsernameContaining(keyword);
        List<UserDTO> userDTOS = new ArrayList<>();
        for(User user:users){
            UserDTO userDTO = UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userDTO);
        }
        logger.debug("Found {} user(s) matching keyword={}", userDTOS.size(), keyword);
        return userDTOS;
    }

    /**
     * Thay đổi trạng thái hoạt động (active/inactive) của một người dùng.
     * <p>
     * Nếu người dùng hiện ở trạng thái hoạt động, sẽ chuyển sang vô hiệu hóa
     * (và ngược lại). Đây là chức năng để admin kiểm soát tài khoản người dùng.
     *
     * @param id id của người dùng cần thay đổi trạng thái
     * @throws UserNotFoundException nếu không tìm thấy người dùng với id tương ứng
     * @throws DataAccessFailureException nếu xảy ra lỗi khi lưu dữ liệu
     */
    public void changeActiveStatusForUser(Long id){
        logger.info("Attempting to change active status for user, id={}", id);
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            User actualUser = user.get();
            actualUser.setActive(!actualUser.isActive());
            try{
                userRepository.save(actualUser);
                logger.info("Active status changed successfully, id={}, newStatus={}", id, actualUser.isActive());
            }catch (Exception e){
                logger.error("Error occurred while changing active status for user, id={}", id, e);
                throw new DataAccessFailureException(e);
            }
        }else{
            logger.warn("Change active status failed: user not found, id={}", id);
            throw new UserNotFoundException(id);
        }
    }

    //CHATBOX

    /**
     * Lấy danh sách tất cả các chatbox trong hệ thống (kể cả những chatbox không còn hoạt động).
     *
     * @return danh sách {@link ChatBoxDTO} của tất cả chatbox;
     *         trả về danh sách rỗng nếu không có chatbox nào
     */
    public List<ChatBoxDTO> getAllChatBox(){
        logger.debug("Fetching all chatboxes");
        List<ChatBox> chatBoxes=chatBoxRepository.findAll();
        List<ChatBoxDTO> chatBoxDTOS=new ArrayList<>();
        for(ChatBox chatBox:chatBoxes){
            ChatBoxDTO chatBoxDTO= ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }
        logger.debug("Found {} chatbox(es)", chatBoxDTOS.size());
        return chatBoxDTOS;
    }

    /**
     * Lấy danh sách tất cả các chatbox mà một người dùng tham gia,
     * sắp xếp giảm dần theo thời gian hoạt động gần nhất.
     *
     * @param userId id của người dùng cần truy vấn danh sách chatbox
     * @return danh sách {@link ChatBoxDTO} của tất cả chatbox mà người dùng tham gia
     *         (kể cả những chatbox không còn hoạt động); trả về danh sách rỗng
     *         nếu người dùng không tham gia chatbox nào
     */
    public List<ChatBoxDTO> getAllUserChatBox(Long userId){
        logger.debug("Fetching all chatboxes for userId={}", userId);
        List<ChatBox> chatBoxes=chatBoxRepository.findByUserIdOrderByLastActiveTimeDesc(userId);
        List<ChatBoxDTO> chatBoxDTOS=new ArrayList<>();
        for(ChatBox chatBox:chatBoxes){
            ChatBoxDTO chatBoxDTO= ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }
        logger.debug("Found {} chatbox(es) for userId={}", chatBoxDTOS.size(), userId);
        return chatBoxDTOS;
    }

    //STATISTICS

    /**
     * Đếm tổng số người dùng trong hệ thống.
     *
     * @return số lượng người dùng (kể cả những tài khoản đã bị vô hiệu hóa)
     */
    public Long countUsers(){
        logger.debug("Counting total users");
        Long count = userRepository.count();
        logger.debug("Total users count={}", count);
        return count;
    }

    /**
     * Đếm số lượng người dùng theo trạng thái hoạt động.
     *
     * @param isActive trạng thái hoạt động ({@code true} = hoạt động, {@code false} = vô hiệu hóa)
     * @return số lượng người dùng có trạng thái tương ứng
     */
    public Long countUsersByActiveStatus(Boolean isActive){
        logger.debug("Counting users by active status, isActive={}", isActive);
        Long count = userRepository.countByIsActive(isActive);
        logger.debug("Users count for isActive={} is {}", isActive, count);
        return count;
    }

    /**
     * Đếm tổng số chatbox trong hệ thống.
     *
     * @return số lượng chatbox (kể cả những chatbox không còn hoạt động)
     */
    public Long countChatBoxes(){
        logger.debug("Counting total chatboxes");
        Long count = chatBoxRepository.count();
        logger.debug("Total chatboxes count={}", count);
        return count;
    }

    /**
     * Đếm số lượng chatbox có thời gian hoạt động gần nhất nằm trong khoảng thời gian nhất định.
     *
     * @param startTime thời gian bắt đầu của khoảng thời gian (inclusive)
     * @param endTime   thời gian kết thúc của khoảng thời gian (inclusive)
     * @return số lượng chatbox có {@code lastActiveTime} nằm trong khoảng thời gian đã chỉ định
     */
    public Long countChatBoxesByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Counting chatboxes with lastActiveTime between startTime={} and endTime={}", startTime, endTime);
        Long count = chatBoxRepository.countByLastActiveTimeBetween(startTime,endTime);
        logger.debug("Chatboxes count in given time range={}", count);
        return count;
    }

    /**
     * Đếm tổng số tin nhắn trong hệ thống.
     *
     * @return số lượng tin nhắn trong tất cả chatbox
     */
    public Long countMessages(){
        logger.debug("Counting total messages");
        Long count = messageRepository.count();
        logger.debug("Total messages count={}", count);
        return count;
    }

    /**
     * Đếm số lượng tin nhắn có thời gian gửi nằm trong khoảng thời gian nhất định.
     *
     * @param startTime thời gian bắt đầu của khoảng thời gian (inclusive)
     * @param endTime   thời gian kết thúc của khoảng thời gian (inclusive)
     * @return số lượng tin nhắn có {@code timestamp} nằm trong khoảng thời gian đã chỉ định
     */
    public Long countMessagesByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Counting messages with timestamp between startTime={} and endTime={}", startTime, endTime);
        Long count = messageRepository.countByTimestampBetween(startTime,endTime);
        logger.debug("Messages count in given time range={}", count);
        return count;
    }
}