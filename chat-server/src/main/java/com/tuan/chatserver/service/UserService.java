package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.UserDTO;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer xử lý nghiệp vụ liên quan tới {@link User}: đăng ký, đăng nhập,
 * quản lý thông tin cá nhân và tìm kiếm người dùng.
 * <p>
 * Toàn bộ mật khẩu được mã hóa bằng {@link BCryptPasswordEncoder} trước khi lưu
 * xuống database. Các thao tác thay đổi dữ liệu trên tài khoản (đổi mật khẩu,
 * cập nhật hồ sơ, xóa tài khoản) đều dựa trên {@code id} làm định danh chính,
 * vì {@code username}/{@code email} có thể thay đổi theo thời gian.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

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
     * @return {@code true} nếu đăng nhập thành công, ngược lại {@code false}
     */
    public boolean loginByUsername(String username, String password){
        Optional<User> user = userRepository.findByUsername(username);
        if(user.isPresent()){
            User actualUser = user.get();
            if(bCryptPasswordEncoder.matches(password,actualUser.getPassword())){
                if(actualUser.isActive()){
                    return true;
                }else{
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
     * Xác thực đăng nhập bằng email và mật khẩu.
     * <p>
     * Đăng nhập thất bại nếu email không tồn tại, mật khẩu không khớp,
     * hoặc tài khoản đã bị vô hiệu hóa ({@code isActive = false}).
     *
     * @param email email của tài khoản
     * @param password mật khẩu dạng plain text do người dùng nhập
     * @return {@code true} nếu đăng nhập thành công, ngược lại {@code false}
     */
    public boolean loginByEmail(String email, String password){
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            User actualUser = user.get();
            if(bCryptPasswordEncoder.matches(password,actualUser.getPassword())){
                if(actualUser.isActive()){
                    return true;
                }else{
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
     * Đăng ký tài khoản người dùng mới.
     * <p>
     * Đăng ký thất bại nếu {@code username} hoặc {@code email} đã tồn tại.
     * Mật khẩu được mã hóa trước khi lưu. Tài khoản mới mặc định ở trạng thái
     * {@code isActive = true}.
     *
     * @param fullname họ tên đầy đủ
     * @param username username duy nhất, dùng để đăng nhập
     * @param email email duy nhất, dùng để đăng nhập
     * @param password mật khẩu dạng plain text, sẽ được mã hóa trước khi lưu
     * @param phone số điện thoại
     * @return {@code true} nếu đăng ký thành công, {@code false} nếu username/email
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
     * Lấy thông tin hồ sơ (profile) của một người dùng theo {@code id}.
     *
     * @param id id của người dùng cần lấy thông tin
     * @return {@link UserDTO} chứa thông tin công khai của người dùng
     *         (không bao gồm mật khẩu), hoặc {@code null} nếu không tìm thấy
     */
    public UserDTO getProfile(Long id){
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            User actualUser = user.get();
            Long userId=actualUser.getId();
            String userName=actualUser.getUsername();
            boolean isActive=actualUser.isActive();
            String fullname=actualUser.getFullname();
            String email=actualUser.getEmail();
            String phone=actualUser.getPhone();
            UserDTO userProfile = new UserDTO(userId,userName,fullname,email,phone,isActive);
            return userProfile;
        }else{
            return null;
        }
    }

    /**
     * Đổi mật khẩu cho một tài khoản đang hoạt động.
     * <p>
     * Yêu cầu xác thực đúng mật khẩu cũ trước khi cho phép đổi. Thất bại nếu
     * tài khoản không tồn tại, đã bị vô hiệu hóa, hoặc mật khẩu cũ không khớp.
     *
     * @param id id của người dùng
     * @param oldPassword mật khẩu hiện tại, dùng để xác thực
     * @param newPassword mật khẩu mới, sẽ được mã hóa trước khi lưu
     * @return {@code true} nếu đổi mật khẩu thành công, ngược lại {@code false}
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
     * Cập nhật thông tin hồ sơ của một tài khoản đang hoạt động, bao gồm
     * cho phép đổi {@code username} và {@code email}.
     * <p>
     * Nếu {@code username} hoặc {@code email} mới khác giá trị hiện tại,
     * sẽ kiểm tra trùng lặp với các tài khoản khác trước khi cập nhật.
     * Thất bại nếu tài khoản không tồn tại, đã bị vô hiệu hóa, hoặc
     * username/email mới đã được người khác sử dụng.
     *
     * @param id id của người dùng cần cập nhật
     * @param fullname họ tên đầy đủ mới
     * @param username username mới (có thể giữ nguyên username hiện tại)
     * @param email email mới (có thể giữ nguyên email hiện tại)
     * @param phone số điện thoại mới
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}
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
     * Dữ liệu tài khoản không bị xóa khỏi database, chỉ chuyển trạng thái,
     * nhằm giữ lại lịch sử liên quan (tin nhắn, nhóm chat, ...).
     *
     * @param id id của người dùng cần vô hiệu hóa
     * @return {@code true} nếu thao tác thành công, ngược lại {@code false}
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
     * Tìm danh sách người dùng đang hoạt động có username chứa từ khóa.
     * <p>
     * Chỉ trả về các tài khoản có {@code isActive = true}; tài khoản đã bị
     * vô hiệu hóa sẽ không xuất hiện trong kết quả.
     *
     * @param keyword từ khóa tìm kiếm trong username
     * @return danh sách {@link UserDTO} của các người dùng đang hoạt động khớp từ khóa
     */
    public List<UserDTO> getActiveUserList(String keyword){
        List<User> users= userRepository.findByUsernameContainingAndIsActiveTrue(keyword);
        List<UserDTO> userDTOs=new ArrayList<>();
        for(User user:users){
            Long id=user.getId();
            String username=user.getUsername();
            String fullname=user.getFullname();
            String email=user.getEmail();
            String phone=user.getPhone();
            UserDTO userDTO=new UserDTO(id, fullname, username, email, phone, true);
            userDTOs.add(userDTO);
        }
        return userDTOs;
    }
}