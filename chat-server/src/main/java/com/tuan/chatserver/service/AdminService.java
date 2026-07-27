//TODO: Add some features
package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.AdminDTO;
import com.tuan.chatserver.dto.AdminRegisterRequest;
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

@Service
public class AdminService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ChatBoxRepository chatBoxRepository;
    private final MessageRepository messageRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    @Autowired
    public AdminService(UserRepository userRepository, AdminRepository adminRepository, ChatBoxRepository chatBoxRepository, MessageRepository messageRepository, BCryptPasswordEncoder bcryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.chatBoxRepository = chatBoxRepository;
        this.messageRepository = messageRepository;
        this.bcryptPasswordEncoder = bcryptPasswordEncoder;
    }

    //ADMIN

    public void registerAdmin(AdminRegisterRequest adminRegisterRequest) {
        String username = adminRegisterRequest.getUsername();
        String password = adminRegisterRequest.getPassword();
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

    public AdminDTO findAdminByUsername(String username) {
        logger.debug("Fetching admin by username, username={}", username);
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin for username={}", username);
            return adminDTO;
        } else {
            logger.debug("No admin found for username={}", username);
            throw new UserNotFoundException(username);
        }
    }

    public AdminDTO findAdminById(Long id){
        logger.debug("Fetching admin by id, id={}", id);
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin for id={}", id);
            return adminDTO;
        } else {
            logger.debug("No admin found for id={}", id);
            throw new UserNotFoundException(id);
        }
    }

    public AdminDTO getAdminProfile(Long id){
        logger.debug("Fetching admin profile, id={}", id);
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            logger.debug("Found admin profile for id={}", id);
            return adminDTO;
        } else {
            logger.debug("No admin found for id={}", id);
            throw new UserNotFoundException(id);
        }
    }

    //USER

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

    public Long countUsers(){
        logger.debug("Counting total users");
        Long count = userRepository.count();
        logger.debug("Total users count={}", count);
        return count;
    }

    public Long countUsersByActiveStatus(Boolean isActive){
        logger.debug("Counting users by active status, isActive={}", isActive);
        Long count = userRepository.countByIsActive(isActive);
        logger.debug("Users count for isActive={} is {}", isActive, count);
        return count;
    }

    public Long countChatBoxes(){
        logger.debug("Counting total chatboxes");
        Long count = chatBoxRepository.count();
        logger.debug("Total chatboxes count={}", count);
        return count;
    }

    public Long countChatBoxesByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Counting chatboxes with lastActiveTime between startTime={} and endTime={}", startTime, endTime);
        Long count = chatBoxRepository.countByLastActiveTimeBetween(startTime,endTime);
        logger.debug("Chatboxes count in given time range={}", count);
        return count;
    }

    public Long countMessages(){
        logger.debug("Counting total messages");
        Long count = messageRepository.count();
        logger.debug("Total messages count={}", count);
        return count;
    }

    public Long countMessagesByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Counting messages with timestamp between startTime={} and endTime={}", startTime, endTime);
        Long count = messageRepository.countByTimestampBetween(startTime,endTime);
        logger.debug("Messages count in given time range={}", count);
        return count;
    }
}