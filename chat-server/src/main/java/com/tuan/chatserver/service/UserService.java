package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.RegisterRequest;
import com.tuan.chatserver.dto.UpdateProfileRequest;
import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.*;
import com.tuan.chatserver.mapper.UserMapper;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ liên quan đến {@link User} (người dùng hệ thống).
 * <p>
 * Bao gồm các chức năng: đăng ký tài khoản, đăng nhập, quản lý hồ sơ cá nhân
 * (cập nhật thông tin, đổi mật khẩu, xóa tài khoản), và tìm kiếm người dùng.
 * <p>
 * Toàn bộ mật khẩu được mã hóa bằng {@link BCryptPasswordEncoder} trước khi lưu
 * xuống database. Các thao tác thay đổi dữ liệu trên tài khoản đều dựa trên
 * {@code id} làm định danh chính, vì {@code username} và {@code email} có thể
 * thay đổi theo thời gian.
 * <p>
 * <b>Lưu ý về log:</b> mật khẩu (plain text lẫn hash) không bao giờ được ghi log.
 * Chỉ log các định danh không nhạy cảm như {@code id} hoặc {@code username}.
 * <p>
 * <b>Lưu ý về exception:</b> Các method ném custom exception thay vì trả về boolean,
 * để Controller có thể xử lý chi tiết qua {@code @ControllerAdvice}.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Khởi tạo {@code UserService} thông qua Constructor Injection.
     *
     * @param userRepository         repository dùng để thao tác dữ liệu {@link User}
     * @param bCryptPasswordEncoder  encoder dùng để mã hóa và xác thực mật khẩu
     */
    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    /**
     * Xác thực đăng nhập bằng username và mật khẩu.
     * <p>
     * Đăng nhập thành công chỉ khi username tồn tại, mật khẩu khớp, và tài khoản
     * đang hoạt động ({@code isActive = true}).
     * <p>
     * Ném {@link WrongPasswordOrInactiveAccountException} nếu username không tồn tại,
     * mật khẩu không khớp, hoặc tài khoản bị vô hiệu hóa (gộp chung vì lý do bảo mật,
     * không tiết lộ username có tồn tại hay không cho client).
     *
     * @param username username của tài khoản
     * @param password mật khẩu dạng plain text do người dùng nhập
     * @throws WrongPasswordOrInactiveAccountException nếu username không tồn tại,
     *         mật khẩu sai, hoặc tài khoản bị khóa
     */
    public void loginByUsername(String username, String password){
        logger.info("Login attempt by username: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if(user.isPresent()){
            User actualUser = user.get();
            boolean success = bCryptPasswordEncoder.matches(password, actualUser.getPassword()) && actualUser.isActive();
            if(success){
                logger.info("Login successful for username: {}", username);
            }else{
                logger.warn("Login failed for username: {} - wrong password or inactive account", username);
                throw new WrongPasswordOrInactiveAccountException();
            }
        }else{
            logger.warn("Login failed - username not found: {}", username);
            throw new WrongPasswordOrInactiveAccountException();
        }
    }

    /**
     * Xác thực đăng nhập bằng email và mật khẩu.
     * <p>
     * Đăng nhập thành công chỉ khi email tồn tại, mật khẩu khớp, và tài khoản
     * đang hoạt động ({@code isActive = true}).
     * <p>
     * Ném {@link WrongPasswordOrInactiveAccountException} nếu email không tồn tại,
     * mật khẩu không khớp, hoặc tài khoản bị vô hiệu hóa (gộp chung vì lý do bảo mật).
     *
     * @param email email của tài khoản
     * @param password mật khẩu dạng plain text do người dùng nhập
     * @throws WrongPasswordOrInactiveAccountException nếu email không tồn tại,
     *         mật khẩu sai, hoặc tài khoản bị khóa
     */
    public void loginByEmail(String email, String password){
        logger.info("Login attempt by email: {}", email);
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            User actualUser = user.get();
            boolean success = bCryptPasswordEncoder.matches(password, actualUser.getPassword()) && actualUser.isActive();
            if(success){
                logger.info("Login successful for email: {}", email);
            }else{
                logger.warn("Login failed for email: {} - wrong password or inactive account", email);
                throw new WrongPasswordOrInactiveAccountException();
            }
        }else{
            logger.warn("Login failed - email not found: {}", email);
            throw new WrongPasswordOrInactiveAccountException();
        }
    }

    /**
     * Đăng ký tài khoản người dùng mới.
     * <p>
     * Mật khẩu được mã hóa bằng BCrypt trước khi lưu vào database. Tài khoản mới
     * mặc định ở trạng thái hoạt động ({@code isActive = true}).
     * <p>
     * Ném {@link UsernameOrEmailAlreadyExistsException} nếu username hoặc email
     * đã tồn tại trong hệ thống. Ném {@link DataAccessFailureException} nếu có lỗi
     * khi lưu vào database.
     *
     * @param registerRequest {@link RegisterRequest} chứa thông tin đăng ký
     *                       (fullname, username, email, password, phone)
     * @throws UsernameOrEmailAlreadyExistsException nếu username hoặc email đã tồn tại
     * @throws DataAccessFailureException nếu lỗi khi lưu vào database
     */
    public void register(RegisterRequest registerRequest){
        String fullname = registerRequest.getFullname();
        String username = registerRequest.getUsername();
        String email = registerRequest.getEmail();
        String password = registerRequest.getPassword();
        String phone = registerRequest.getPhone();
        logger.info("Register attempt for username: {}, email: {}", username, email);

        if(userRepository.existsByUsername(username) || userRepository.existsByEmail(email)){
            logger.warn("Register failed - username or email already exists: username={}, email={}", username, email);
            throw new UsernameOrEmailAlreadyExistsException();
        }

        String hashedPassword = bCryptPasswordEncoder.encode(password);
        User user = new User(fullname, username, email, hashedPassword, phone, true);
        try{
            userRepository.save(user);
            logger.info("Register successful for username: {}", username);
        }catch(Exception e){
            logger.error("Register failed while saving user: username={}", username, e);
            throw new DataAccessFailureException(e);
        }
    }

    /**
     * Lấy thông tin hồ sơ (profile) công khai của một người dùng theo {@code id}.
     * <p>
     * Phương thức trả về {@link UserDTO} chứa các thông tin cơ bản có thể chia sẻ
     * công khai (không bao gồm mật khẩu).
     *
     * @param id id của người dùng cần lấy thông tin
     * @return {@link UserDTO} chứa thông tin hồ sơ; hoặc {@code null} nếu
     *         không tìm thấy người dùng với id tương ứng
     */
    public UserDTO getProfile(Long id){
        logger.debug("Fetching profile for userId: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            return UserMapper.mapUserToUserDTO(user.get());
        }else{
            logger.warn("Get profile failed - userId not found: {}", id);
            return null;
        }
    }

    /**
     * Đổi mật khẩu cho một tài khoản đang hoạt động.
     * <p>
     * Yêu cầu xác thực đúng mật khẩu cũ (dạng plain text) trước khi cho phép
     * đổi sang mật khẩu mới. Mật khẩu mới sẽ được mã hóa bằng BCrypt trước khi lưu.
     * <p>
     * Ném {@link UserNotFoundException} nếu tài khoản không tồn tại.
     * Ném {@link WrongPasswordOrInactiveAccountException} nếu tài khoản bị vô hiệu hóa
     * hoặc mật khẩu cũ không khớp. Ném {@link DataAccessFailureException} nếu lỗi
     * khi lưu vào database.
     *
     * @param id           id của người dùng cần đổi mật khẩu
     * @param oldPassword  mật khẩu hiện tại (dạng plain text)
     * @param newPassword  mật khẩu mới (dạng plain text, sẽ được hash)
     * @throws UserNotFoundException nếu userId không tồn tại
     * @throws WrongPasswordOrInactiveAccountException nếu tài khoản bị khóa hoặc
     *         mật khẩu cũ không khớp
     * @throws DataAccessFailureException nếu lỗi khi lưu vào database
     */
    public void changePassword(Long id, String oldPassword, String newPassword){
        logger.info("Change password attempt for userId: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            logger.warn("Change password failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }

        User actualUser = user.get();
        if(!actualUser.isActive()){
            logger.warn("Change password failed - account inactive for userId: {}", id);
            throw new WrongPasswordOrInactiveAccountException();
        }

        if(!bCryptPasswordEncoder.matches(oldPassword, actualUser.getPassword())){
            logger.warn("Change password failed - old password mismatch for userId: {}", id);
            throw new WrongPasswordOrInactiveAccountException();
        }

        try{
            actualUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
            userRepository.save(actualUser);
            logger.info("Change password successful for userId: {}", id);
        }catch(Exception e){
            logger.error("Change password failed while saving userId: {}", id, e);
            throw new DataAccessFailureException(e);
        }
    }

    /**
     * Cập nhật thông tin hồ sơ của một tài khoản đang hoạt động.
     * <p>
     * Cho phép cập nhật: họ tên, username, email, và số điện thoại. Nếu username
     * hoặc email mới khác giá trị hiện tại, sẽ kiểm tra trùng lặp với các tài khoản
     * khác trước khi cập nhật.
     * <p>
     * Ném {@link UserNotFoundException} nếu tài khoản không tồn tại.
     * Ném {@link WrongPasswordOrInactiveAccountException} nếu tài khoản bị vô hiệu hóa.
     * Ném {@link UsernameOrEmailAlreadyExistsException} nếu username/email mới
     * đã được người khác sử dụng. Ném {@link DataAccessFailureException} nếu lỗi
     * khi lưu vào database.
     *
     * @param id id của người dùng cần cập nhật (lấy từ URL path)
     * @param updateProfileRequest {@link UpdateProfileRequest} chứa thông tin cập nhật
     *                            (fullname, username, email, phone)
     * @throws UserNotFoundException nếu userId không tồn tại
     * @throws WrongPasswordOrInactiveAccountException nếu tài khoản bị khóa
     * @throws UsernameOrEmailAlreadyExistsException nếu username/email mới bị trùng
     * @throws DataAccessFailureException nếu lỗi khi lưu vào database
     */
    public void updateProfile(Long id, UpdateProfileRequest updateProfileRequest){
        String fullname = updateProfileRequest.getFullname();
        String username = updateProfileRequest.getUsername();
        String email = updateProfileRequest.getEmail();
        String phone = updateProfileRequest.getPhone();
        logger.info("Update profile attempt for userId: {}", id);

        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            logger.warn("Update profile failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }

        User actualUser = user.get();
        if(!actualUser.isActive()){
            logger.warn("Update profile failed - account inactive for userId: {}", id);
            throw new WrongPasswordOrInactiveAccountException();
        }

        boolean usernameAvailable = actualUser.getUsername().equals(username) || !userRepository.existsByUsername(username);
        boolean emailAvailable = actualUser.getEmail().equals(email) || !userRepository.existsByEmail(email);
        if(!usernameAvailable || !emailAvailable){
            logger.warn("Update profile failed - username or email already taken for userId: {}", id);
            throw new UsernameOrEmailAlreadyExistsException();
        }

        actualUser.setFullname(fullname);
        actualUser.setUsername(username);
        actualUser.setEmail(email);
        actualUser.setPhone(phone);
        try{
            userRepository.save(actualUser);
            logger.info("Update profile successful for userId: {}", id);
        }catch(Exception e){
            logger.error("Update profile failed while saving userId: {}", id, e);
            throw new DataAccessFailureException(e);
        }
    }

    /**
     * Vô hiệu hóa tài khoản (soft delete) bằng cách đặt {@code isActive = false}.
     * <p>
     * Dữ liệu tài khoản không bị xóa khỏi database, chỉ chuyển trạng thái thành
     * không hoạt động, nhằm giữ lại lịch sử liên quan (tin nhắn, nhóm chat, ...).
     * Người dùng sẽ không thể đăng nhập lại sau khi tài khoản bị vô hiệu hóa.
     * <p>
     * Ném {@link UserNotFoundException} nếu tài khoản không tồn tại.
     * Ném {@link DataAccessFailureException} nếu lỗi khi lưu vào database.
     *
     * @param id id của người dùng cần vô hiệu hóa
     * @return {@code true} nếu vô hiệu hóa thành công
     * @throws UserNotFoundException nếu userId không tồn tại
     * @throws DataAccessFailureException nếu lỗi khi lưu vào database
     */
    public void deleteAccount(Long id){
        logger.info("Delete account attempt for userId: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            logger.warn("Delete account failed - userId not found: {}", id);
            throw new UserNotFoundException(id);
        }

        User actualUser = user.get();
        actualUser.setActive(false);
        try{
            userRepository.save(actualUser);
            logger.info("Delete account successful for userId: {}", id);
        }catch(Exception e){
            logger.error("Delete account failed while saving userId: {}", id, e);
            throw new DataAccessFailureException(e);
        }
    }

    /**
     * Tìm danh sách người dùng đang hoạt động có username chứa từ khóa tìm kiếm.
     * <p>
     * Chỉ trả về các tài khoản có {@code isActive = true}; tài khoản đã bị
     * vô hiệu hóa sẽ không xuất hiện trong kết quả tìm kiếm.
     *
     * @param keyword từ khóa tìm kiếm trong username (tìm kiếm LIKE, phân biệt hoa/thường)
     * @return danh sách {@link UserDTO} của các người dùng đang hoạt động khớp từ khóa;
     *         trả về danh sách rỗng nếu không có kết quả nào phù hợp
     */
    public List<UserDTO> getActiveUserList(String keyword){
        logger.debug("Searching active users with keyword: {}", keyword);
        List<User> users = userRepository.findByUsernameContainingAndIsActiveTrue(keyword);
        List<UserDTO> userDTOs = new ArrayList<>();
        for(User user : users){
            userDTOs.add(UserMapper.mapUserToUserDTO(user));
        }
        logger.debug("Found {} active user(s) matching keyword: {}", userDTOs.size(), keyword);
        return userDTOs;
    }
}