package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.mapper.UserMapper;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

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
     * Đăng nhập thất bại nếu username không tồn tại, mật khẩu không khớp,
     * hoặc tài khoản đã bị vô hiệu hóa ({@code isActive = false}).
     *
     * @param username username của tài khoản
     * @param password mật khẩu dạng plain text do người dùng nhập
     * @return {@code true} nếu đăng nhập thành công; {@code false} nếu username
     *         không tồn tại, mật khẩu không khớp, hoặc tài khoản bị vô hiệu hóa
     */
    public boolean loginByUsername(String username, String password){
        Optional<User> user = userRepository.findByUsername(username);
        if(user.isPresent()){
            User actualUser = user.get();
            return bCryptPasswordEncoder.matches(password, actualUser.getPassword()) && actualUser.isActive();
        }else{
            return false;
        }
    }

    /**
     * Xác thực đăng nhập bằng email và mật khẩu.
     * <p>
     * Đăng nhập thất bại nếu email không tồn tại, mật khẩu không khớp,
     * hoặc tài khoản đã bị vô hiệu hóa ({@code isActive = false}).
     *
     * @param email email của tài khoản
     * @param password mật khẩu dạng plain text do người dùng nhập
     * @return {@code true} nếu đăng nhập thành công; {@code false} nếu email
     *         không tồn tại, mật khẩu không khớp, hoặc tài khoản bị vô hiệu hóa
     */
    public boolean loginByEmail(String email, String password){
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            User actualUser = user.get();
            return bCryptPasswordEncoder.matches(password, actualUser.getPassword()) && actualUser.isActive();
        }else{
            return false;
        }
    }

    /**
     * Đăng ký tài khoản người dùng mới.
     * <p>
     * Đăng ký thất bại nếu {@code username} hoặc {@code email} đã tồn tại trong hệ thống.
     * Mật khẩu được mã hóa bằng BCrypt trước khi lưu vào database. Tài khoản mới
     * mặc định ở trạng thái hoạt động ({@code isActive = true}).
     *
     * @param fullname   họ tên đầy đủ
     * @param username   username duy nhất, dùng để đăng nhập
     * @param email      email duy nhất, dùng để đăng nhập
     * @param password   mật khẩu dạng plain text, sẽ được mã hóa trước khi lưu
     * @param phone      số điện thoại liên hệ
     * @return {@code true} nếu đăng ký thành công; {@code false} nếu username/email
     *         đã tồn tại hoặc xảy ra lỗi khi lưu dữ liệu
     */
    public boolean register(String fullname, String username, String email, String password, String phone){
        if(userRepository.existsByUsername(username) || userRepository.existsByEmail(email)){
            return false;
        }else{
            String hashedPassword = bCryptPasswordEncoder.encode(password);
            User user=new User(fullname, username, email, hashedPassword, phone, true);
            try{
                userRepository.save(user);
                return true;
            }catch(Exception e){
                e.printStackTrace();
                return false;
            }
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
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            UserDTO userProfile = UserMapper.mapUserToUserDTO(user.get());
            return userProfile;
        }else{
            return null;
        }
    }

    /**
     * Đổi mật khẩu cho một tài khoản đang hoạt động.
     * <p>
     * Yêu cầu xác thực đúng mật khẩu cũ trước khi cho phép đổi sang mật khẩu mới.
     * Mật khẩu mới sẽ được mã hóa bằng BCrypt trước khi lưu. Thất bại nếu tài khoản
     * không tồn tại, đã bị vô hiệu hóa, hoặc mật khẩu cũ không khớp.
     *
     * @param id           id của người dùng cần đổi mật khẩu
     * @param oldPassword  mật khẩu hiện tại (dạng plain text), dùng để xác thực
     * @param newPassword  mật khẩu mới (dạng plain text), sẽ được mã hóa trước khi lưu
     * @return {@code true} nếu đổi mật khẩu thành công; {@code false} nếu tài khoản
     *         không tồn tại, bị vô hiệu hóa, mật khẩu cũ không khớp, hoặc xảy ra lỗi khi lưu
     */
    public boolean changePassword(Long id, String oldPassword, String newPassword){
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            User actualUser = user.get();
            if(actualUser.isActive()){
                if(bCryptPasswordEncoder.matches(oldPassword,actualUser.getPassword())){
                    try{
                        actualUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
                        userRepository.save(actualUser);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }
                else{
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
     * Cập nhật thông tin hồ sơ của một tài khoản đang hoạt động.
     * <p>
     * Cho phép cập nhật: họ tên, username, email, và số điện thoại. Nếu username
     * hoặc email mới khác giá trị hiện tại, sẽ kiểm tra trùng lặp với các tài khoản
     * khác trước khi cập nhật. Thất bại nếu tài khoản không tồn tại, đã bị vô hiệu hóa,
     * hoặc username/email mới đã được người khác sử dụng.
     *
     * @param id       id của người dùng cần cập nhật
     * @param fullname họ tên đầy đủ mới
     * @param username username mới (có thể giữ nguyên username hiện tại)
     * @param email    email mới (có thể giữ nguyên email hiện tại)
     * @param phone    số điện thoại mới
     * @return {@code true} nếu cập nhật thành công; {@code false} nếu tài khoản
     *         không tồn tại, bị vô hiệu hóa, username/email mới bị trùng, hoặc xảy ra lỗi khi lưu
     */
    public boolean updateProfile(Long id, String fullname, String username, String email, String phone){
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            User actualUser = user.get();
            if(!actualUser.isActive()){
                return false;
            }else{
                boolean usernameAvailable = actualUser.getUsername().equals(username) || !userRepository.existsByUsername(username);
                boolean emailAvailable = actualUser.getEmail().equals(email) || !userRepository.existsByEmail(email);
                if(usernameAvailable && emailAvailable){
                    actualUser.setFullname(fullname);
                    actualUser.setUsername(username);
                    actualUser.setEmail(email);
                    actualUser.setPhone(phone);
                    try{
                        userRepository.save(actualUser);
                        return true;
                    }catch(Exception e){
                        e.printStackTrace();
                        return false;
                    }
                }else{
                    return false;
                }
            }
        }else{
            return false;
        }
    }

    /**
     * Vô hiệu hóa tài khoản (soft delete) bằng cách đặt {@code isActive = false}.
     * <p>
     * Dữ liệu tài khoản không bị xóa khỏi database, chỉ chuyển trạng thái thành
     * không hoạt động, nhằm giữ lại lịch sử liên quan (tin nhắn, nhóm chat, ...).
     * Người dùng sẽ không thể đăng nhập lại sau khi tài khoản bị vô hiệu hóa.
     *
     * @param id id của người dùng cần vô hiệu hóa
     * @return {@code true} nếu vô hiệu hóa thành công; {@code false} nếu
     *         tài khoản không tồn tại hoặc xảy ra lỗi khi lưu
     */
    public boolean deleteAccount(Long id){
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            User actualUser = user.get();
            actualUser.setActive(false);
            try{
                userRepository.save(actualUser);
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
        List<User> users= userRepository.findByUsernameContainingAndIsActiveTrue(keyword);
        List<UserDTO> userDTOs=new ArrayList<>();
        for(User user:users){
            UserDTO userDTO=UserMapper.mapUserToUserDTO(user);
            userDTOs.add(userDTO);
        }
        return userDTOs;
    }
}