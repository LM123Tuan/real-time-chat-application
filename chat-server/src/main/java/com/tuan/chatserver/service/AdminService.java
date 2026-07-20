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

@Service
public class AdminService {
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

    public AdminDTO findAdminByUsername(String username) {
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            AdminDTO adminDTO = AdminMapper.mapAdminToAdminDTO(admin.get());
            return adminDTO;
        } else {
            return null;
        }
    }

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

    public List<UserDTO> findAllUsers(){
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOS = new ArrayList<>();
        for(User user:users){
            UserDTO userDTO = UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }

    public List<UserDTO> findUserByUsernameContaining(String keyword){
        List<User> users = userRepository.findByUsernameContaining(keyword);
        List<UserDTO> userDTOS = new ArrayList<>();
        for(User user:users){
            UserDTO userDTO = UserMapper.mapUserToUserDTO(user);
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }

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

    public List<ChatBoxDTO> getAllChatBox(){
        List<ChatBox> chatBoxes=chatBoxRepository.findAll();
        List<ChatBoxDTO> chatBoxDTOS=new ArrayList<>();
        for(ChatBox chatBox:chatBoxes){
            ChatBoxDTO chatBoxDTO= ChatBoxMapper.mapChatBoxToChatBoxDTO(chatBox);
            chatBoxDTOS.add(chatBoxDTO);
        }
        return chatBoxDTOS;
    }

    //STATISTICS

    public Long countUsers(){
        return userRepository.count();
    }

    public Long countUsersByActiveStatus(Boolean isActive){
        return userRepository.countByIsActive(isActive);
    }

    public Long countChatBoxes(){
        return chatBoxRepository.count();
    }

    public Long countChatBoxesByLastActiveTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return chatBoxRepository.countByLastActiveTimeBetween(startTime,endTime);
    }

    public Long countMessages(){
        return messageRepository.count();
    }

    public Long countMessagesByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return messageRepository.countByTimestampBetween(startTime,endTime);
    }
}
