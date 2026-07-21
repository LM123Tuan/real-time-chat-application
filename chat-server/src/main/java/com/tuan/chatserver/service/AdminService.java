//TODO
package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.AdminDTO;
import com.tuan.chatserver.dto.ChatBoxDTO;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.mapper.AdminMapper;
import com.tuan.chatserver.mapper.ChatBoxMapper;
import com.tuan.chatserver.mapper.UserMapper;
import com.tuan.chatserver.repository.AdminRepository;
import com.tuan.chatserver.repository.ChatBoxRepository;
import com.tuan.chatserver.repository.MessageRepository;
import com.tuan.chatserver.repository.UserRepository;
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
     * @return {@code true} nếu đăng ký thành công; {@code false} nếu username
     *         đã tồn tại hoặc xảy ra lỗi khi lưu dữ liệu
     */
    public boolean registerAdmin(String username, String password) {
        String hashedPassword = bcryptPasswordEncoder.encode(password);
        if(!adminRepository.existsByUsername(username)) {
            Admin admin = new Admin(username, hashedPassword, true);
            try{
                adminRepository.save(admin);
                return true;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
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
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            return adminDTO;
        } else {
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
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            return adminDTO;
        } else {
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
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOS = new ArrayList<>();
        for(User user:users){
            UserDTO userDTO = UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userDTO);
        }
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
        List<User> users = userRepository.findByUsernameContaining(keyword);
        List<UserDTO> userDTOS = new ArrayList<>();
        for(User user:users){
            UserDTO userDTO = UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }

    /**
     * Thay đổi trạng thái hoạt động (active/inactive) của một người dùng.
     * <p>
     * Nếu người dùng hiện ở trạng thái hoạt động, sẽ chuyển sang vô hiệu hóa
     * (và ngược lại). Đây là chức năng để admin kiểm soát tài khoản người dùng.
     *
     * @param id id của người dùng cần thay đổi trạng thái
     * @return {@code true} nếu thay đổi và lưu thành công; {@code false} nếu
     *         người dùng không tồn tại hoặc xảy ra lỗi khi lưu
     */
    public boolean changeActiveStatusForUser(Long id){
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            User actualUser = user.get();
            actualUser.setActive(!actualUser.isActive());
            try{
                userRepository.save(actualUser);
                return true;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
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
        List<ChatBox> chatBoxes=chatBoxRepository.findAll();
        List<ChatBoxDTO> chatBoxDTOS=new ArrayList<>();
        for(ChatBox chatBox:chatBoxes){
            ChatBoxDTO chatBoxDTO= ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }
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
        List<ChatBox> chatBoxes=chatBoxRepository.findByUserIdOrderByLastActiveTimeDesc(userId);
        List<ChatBoxDTO> chatBoxDTOS=new ArrayList<>();
        for(ChatBox chatBox:chatBoxes){
            ChatBoxDTO chatBoxDTO= ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }
        return chatBoxDTOS;
    }

    //STATISTICS

    /**
     * Đếm tổng số người dùng trong hệ thống.
     *
     * @return số lượng người dùng (kể cả những tài khoản đã bị vô hiệu hóa)
     */
    public Long countUsers(){
        return userRepository.count();
    }

    /**
     * Đếm số lượng người dùng theo trạng thái hoạt động.
     *
     * @param isActive trạng thái hoạt động ({@code true} = hoạt động, {@code false} = vô hiệu hóa)
     * @return số lượng người dùng có trạng thái tương ứng
     */
    public Long countUsersByActiveStatus(Boolean isActive){
        return userRepository.countByIsActive(isActive);
    }

    /**
     * Đếm tổng số chatbox trong hệ thống.
     *
     * @return số lượng chatbox (kể cả những chatbox không còn hoạt động)
     */
    public Long countChatBoxes(){
        return chatBoxRepository.count();
    }

    /**
     * Đếm số lượng chatbox có thời gian hoạt động gần nhất nằm trong khoảng thời gian nhất định.
     *
     * @param startTime thời gian bắt đầu của khoảng thời gian (inclusive)
     * @param endTime   thời gian kết thúc của khoảng thời gian (inclusive)
     * @return số lượng chatbox có {@code lastActiveTime} nằm trong khoảng thời gian đã chỉ định
     */
    public Long countChatBoxesByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return chatBoxRepository.countByLastActiveTimeBetween(startTime,endTime);
    }

    /**
     * Đếm tổng số tin nhắn trong hệ thống.
     *
     * @return số lượng tin nhắn trong tất cả chatbox
     */
    public Long countMessages(){
        return messageRepository.count();
    }

    /**
     * Đếm số lượng tin nhắn có thời gian gửi nằm trong khoảng thời gian nhất định.
     *
     * @param startTime thời gian bắt đầu của khoảng thời gian (inclusive)
     * @param endTime   thời gian kết thúc của khoảng thời gian (inclusive)
     * @return số lượng tin nhắn có {@code timestamp} nằm trong khoảng thời gian đã chỉ định
     */
    public Long countMessagesByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return messageRepository.countByTimestampBetween(startTime,endTime);
    }
}