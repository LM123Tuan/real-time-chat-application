//TODO: Add some features
package com.tuan.chatserver.service;

import com.tuan.chatserver.dto.*;
import com.tuan.chatserver.entity.Admin;
import com.tuan.chatserver.entity.ChatBox;
import com.tuan.chatserver.entity.User;
import com.tuan.chatserver.exception.AdminAccessDeniedException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    private void validateAdminAccess(Long requesterId) {
        logger.debug("Validating admin access, requesterId={}", requesterId);
        if (!adminRepository.existsById(requesterId)) {
            logger.warn("Access denied: requester is not an admin, requesterId={}", requesterId);
            throw new AdminAccessDeniedException(requesterId);
        }
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

    public AdminDTO findAdminByUsername(Long requesterId, String username) {
        validateAdminAccess(requesterId);
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

    public AdminDTO findAdminById(Long requesterId, Long id){
        validateAdminAccess(requesterId);
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

    public AdminDTO getAdminProfile(Long requesterId, Long id){
        validateAdminAccess(requesterId);
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

    public CursorPaginationResponse<List<OtherProfileDTO>, Long> findAllUsers(Long requesterId, CursorPaginationRequest<Long> request) {
        validateAdminAccess(requesterId);
        logger.debug("Fetching all users");

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<User> users;
        if (request.getCursorId() == null) {
            users = userRepository.findAllOfFirstPage(pageable);
        } else {
            users = userRepository.findAllOfNextPage(request.getCursorId(), pageable);
        }

        boolean hasNext = users.size() > request.getSize();
        if (hasNext) {
            users = users.subList(0, request.getSize());
        }

        List<OtherProfileDTO> profileDTOs = new ArrayList<>();
        for (User user : users) {
            profileDTOs.add(UserMapper.mapUserToOtherUserDTO(user));
        }

        Long nextCursor = null;
        if (!users.isEmpty()) {
            nextCursor = users.get(users.size() - 1).getId();
        }

        CursorPaginationResponse<List<OtherProfileDTO>, Long> response =
                new CursorPaginationResponse<>(profileDTOs, null, nextCursor, hasNext);

        logger.debug("Found {} user(s)", profileDTOs.size());
        return response;
    }

    public CursorPaginationResponse<List<OtherProfileDTO>, Long> findUserByUsernameContaining(
            Long requesterId, String keyword, CursorPaginationRequest<Long> request) {

        validateAdminAccess(requesterId);
        logger.debug("Fetching users by username containing keyword, keyword={}", keyword);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<User> users;
        if (request.getCursorId() == null) {
            users = userRepository.findByUsernameContainingOfFirstPage(keyword, pageable);
        } else {
            users = userRepository.findByUsernameContainingOfNextPage(keyword, request.getCursorId(), pageable);
        }

        boolean hasNext = users.size() > request.getSize();
        if (hasNext) {
            users = users.subList(0, request.getSize());
        }

        List<OtherProfileDTO> profileDTOs = new ArrayList<>();
        for (User user : users) {
            profileDTOs.add(UserMapper.mapUserToOtherUserDTO(user));
        }

        Long nextCursor = null;
        if (!users.isEmpty()) {
            nextCursor = users.get(users.size() - 1).getId();
        }

        CursorPaginationResponse<List<OtherProfileDTO>, Long> response =
                new CursorPaginationResponse<>(profileDTOs, null, nextCursor, hasNext);

        logger.debug("Found {} user(s) matching keyword={}", profileDTOs.size(), keyword);
        return response;
    }

    public void changeActiveStatusForUser(Long requesterId, Long id){
        validateAdminAccess(requesterId);
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

    public CursorPaginationResponse<List<ChatBoxDTO>, Long> getAllChatBox(Long requesterId, CursorPaginationRequest<Long> request) {
        validateAdminAccess(requesterId);
        logger.debug("Fetching all chatboxes");

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<ChatBox> chatBoxes;
        if (request.getCursorId() == null) {
            chatBoxes = chatBoxRepository.findAllOfFirstPage(pageable);
        } else {
            chatBoxes = chatBoxRepository.findAllOfNextPage(request.getCursorId(), pageable);
        }

        boolean hasNext = chatBoxes.size() > request.getSize();
        if (hasNext) {
            chatBoxes = chatBoxes.subList(0, request.getSize());
        }

        Set<Long> chatBoxIds = chatBoxes.stream()
                .map(ChatBox::getId)
                .collect(Collectors.toSet());

        List<ChatBox> confirmedChatBoxes = chatBoxIds.isEmpty()
                ? List.of()
                : chatBoxRepository.findByIdInWithUsers(new ArrayList<>(chatBoxIds));

        Map<Long, ChatBox> confirmedChatBoxMap = confirmedChatBoxes.stream()
                .collect(Collectors.toMap(ChatBox::getId, cb -> cb));

        List<ChatBoxDTO> chatBoxDTOs = new ArrayList<>();
        for (ChatBox chatBox : chatBoxes) {
            ChatBox confirmed = confirmedChatBoxMap.get(chatBox.getId());
            if (confirmed != null) {
                chatBoxDTOs.add(ChatBoxMapper.mapChatBoxToChatBoxDTO(confirmed));
            }
        }

        Long nextCursor = null;
        if (!chatBoxes.isEmpty()) {
            nextCursor = chatBoxes.get(chatBoxes.size() - 1).getId();
        }

        CursorPaginationResponse<List<ChatBoxDTO>, Long> response =
                new CursorPaginationResponse<>(chatBoxDTOs, null, nextCursor, hasNext);

        logger.debug("Found {} chatbox(es)", chatBoxDTOs.size());
        return response;
    }

    public CursorPaginationResponse<List<ChatBoxDTO>, Long> getAllUserChatBox(
            Long requesterId, Long userId, CursorPaginationRequest<Long> request) {

        validateAdminAccess(requesterId);
        logger.debug("Fetching all chatboxes for userId={}", userId);

        Pageable pageable = PageRequest.of(0, request.getSize() + 1);

        List<ChatBox> chatBoxes;
        if (request.getCursorTimestamp() == null) {
            chatBoxes = chatBoxRepository.findByUserIdOfFirstPage(userId, pageable);
        } else {
            chatBoxes = chatBoxRepository.findByUserIdOfNextPage(
                    userId, request.getCursorTimestamp(), request.getCursorId(), pageable);
        }

        boolean hasNext = chatBoxes.size() > request.getSize();
        if (hasNext) {
            chatBoxes = chatBoxes.subList(0, request.getSize());
        }

        Set<Long> chatBoxIds = chatBoxes.stream()
                .map(ChatBox::getId)
                .collect(Collectors.toSet());

        List<ChatBox> confirmedChatBoxes = chatBoxIds.isEmpty()
                ? List.of()
                : chatBoxRepository.findByIdInWithUsers(new ArrayList<>(chatBoxIds));

        Map<Long, ChatBox> confirmedChatBoxMap = confirmedChatBoxes.stream()
                .collect(Collectors.toMap(ChatBox::getId, cb -> cb));

        List<ChatBoxDTO> chatBoxDTOs = new ArrayList<>();
        for (ChatBox chatBox : chatBoxes) {
            ChatBox confirmed = confirmedChatBoxMap.get(chatBox.getId());
            if (confirmed != null) {
                chatBoxDTOs.add(ChatBoxMapper.mapChatBoxToChatBoxDTO(confirmed));
            }
        }

        LocalDateTime nextTimestamp = null;
        Long nextCursor = null;
        if (!chatBoxes.isEmpty()) {
            ChatBox lastChatBox = chatBoxes.get(chatBoxes.size() - 1);
            nextTimestamp = lastChatBox.getLastActiveTime();
            nextCursor = lastChatBox.getId();
        }

        CursorPaginationResponse<List<ChatBoxDTO>, Long> response =
                new CursorPaginationResponse<>(chatBoxDTOs, nextTimestamp, nextCursor, hasNext);

        logger.debug("Found {} chatbox(es) for userId={}", chatBoxDTOs.size(), userId);
        return response;
    }

    //STATISTICS

    public Long countUsers(Long requesterId){
        validateAdminAccess(requesterId);
        logger.debug("Counting total users");
        Long count = userRepository.count();
        logger.debug("Total users count={}", count);
        return count;
    }

    public Long countUsersByActiveStatus(Long requesterId, Boolean isActive){
        validateAdminAccess(requesterId);
        logger.debug("Counting users by active status, isActive={}", isActive);
        Long count = userRepository.countByIsActive(isActive);
        logger.debug("Users count for isActive={} is {}", isActive, count);
        return count;
    }

    public Long countChatBoxes(Long requesterId){
        validateAdminAccess(requesterId);
        logger.debug("Counting total chatboxes");
        Long count = chatBoxRepository.count();
        logger.debug("Total chatboxes count={}", count);
        return count;
    }

    public Long countChatBoxesByLastActiveTimeBetween(Long requesterId, LocalDateTime startTime, LocalDateTime endTime) {
        validateAdminAccess(requesterId);
        logger.debug("Counting chatboxes with lastActiveTime between startTime={} and endTime={}", startTime, endTime);
        Long count = chatBoxRepository.countByLastActiveTimeBetween(startTime,endTime);
        logger.debug("Chatboxes count in given time range={}", count);
        return count;
    }

    public Long countMessages(Long requesterId){
        validateAdminAccess(requesterId);
        logger.debug("Counting total messages");
        Long count = messageRepository.count();
        logger.debug("Total messages count={}", count);
        return count;
    }

    public Long countMessagesByTimestampBetween(Long requesterId, LocalDateTime startTime, LocalDateTime endTime) {
        validateAdminAccess(requesterId);
        logger.debug("Counting messages with timestamp between startTime={} and endTime={}", startTime, endTime);
        Long count = messageRepository.countByTimestampBetween(startTime,endTime);
        logger.debug("Messages count in given time range={}", count);
        return count;
    }
}